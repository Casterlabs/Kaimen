package co.casterlabs.kaimen.webview.impl.webkit;

import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.ProgressEvent;
import org.eclipse.swt.browser.ProgressListener;
import org.eclipse.swt.browser.TitleEvent;
import org.eclipse.swt.browser.TitleListener;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.jetbrains.annotations.Nullable;

import co.casterlabs.kaimen.app.App;
import co.casterlabs.kaimen.util.platform.Arch;
import co.casterlabs.kaimen.util.platform.OperatingSystem;
import co.casterlabs.kaimen.util.platform.Platform;
import co.casterlabs.kaimen.util.threading.AsyncTask;
import co.casterlabs.kaimen.util.threading.MainThread;
import co.casterlabs.kaimen.util.threading.MainThreadPromise;
import co.casterlabs.kaimen.webview.Webview;
import co.casterlabs.kaimen.webview.WebviewFactory;
import co.casterlabs.kaimen.webview.WebviewRenderer;
import co.casterlabs.kaimen.webview.WebviewWindowProperties;
import co.casterlabs.kaimen.webview.bridge.WebviewBridge;
import co.casterlabs.rakurai.json.Rson;
import co.casterlabs.rakurai.json.element.JsonArray;
import co.casterlabs.rakurai.json.element.JsonElement;
import co.casterlabs.rakurai.json.serialization.JsonParseException;
import lombok.Getter;
import lombok.NonNull;
import lombok.SneakyThrows;
import xyz.e3ndr.fastloggingframework.logging.FastLogger;
import xyz.e3ndr.reflectionlib.ReflectionLib;

public class WkWebview extends Webview {
    private static boolean initialized = false;
    private static Display display;

    public static final WebviewFactory FACTORY = new WebviewFactory() {

        @Override
        public @Nullable Webview produce() throws Exception {
            return new WkWebview();
        }

        @Override
        public Map<OperatingSystem, List<Arch>> getSupportMap() {
            Map<OperatingSystem, List<Arch>> supported = new HashMap<>();

            supported.put(
                OperatingSystem.MACOSX,
                Arrays.asList(Arch.AARCH64, Arch.AMD64)
            );

            return supported;
        };

        @Override
        public WebviewRenderer getRendererType() {
            return WebviewRenderer.WEBKIT;
        }

        @Override
        public boolean supportsTransparency() {
            return false;
        };

    };

    static {
        // Required for Linux: https://bugs.eclipse.org/bugs/show_bug.cgi?id=161911"
        System.setProperty("sun.awt.xembedserver", "true");
    }

    private WkBridge bridge = new WkBridge(this);
    private boolean hasPreloaded = false;

    private Browser browser;
    private Shell shell;

    private @Getter String pageTitle;

    @SneakyThrows
    @Override
    protected void initialize0() {
        if (this.isTransparencyEnabled()) {
            throw new UnsupportedOperationException("Transparency is not supported on macOS at this time.");
        }

        if (!initialized) {
            initialized = true;
            MainThread.submitTask(() -> {
                if (display == null) {
                    display = Display.getCurrent();
                }
            });
        }

        MainThread.submitTaskAndWait(this::mt_initialize);

        this.changeImage(App.getIconURL());
    }

    private void mt_initialize() {
        this.shell = new Shell(display, SWT.SHELL_TRIM);
        this.shell.setLayout(new FillLayout());

        this.browser = new Browser(this.shell, SWT.WEBKIT);

        this.browser.setUrl("about:blank");

        try {
            if (Platform.os == OperatingSystem.MACOSX) {
                Object webkit = ReflectionLib.getValue(browser, "webBrowser"); // org.eclipse.swt.browser.WebKit
                Object view = ReflectionLib.getValue(webkit, "webView"); // org.eclipse.swt.internal.cocoa.WebView

                Object ns_applicationName = ReflectionLib.invokeStaticMethod(
                    Class.forName("org.eclipse.swt.internal.cocoa.NSString"),
                    "stringWith",
                    String.format("Safari/522.0 Kaimen (%s)", Webview.getWebviewToken())
                );

                ReflectionLib.invokeMethod(view, "setApplicationNameForUserAgent", ns_applicationName);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        this.browser.addProgressListener(new ProgressListener() {
            @Override
            public void changed(ProgressEvent event) {
//                FastLogger.logStatic(event);

                if (event.current == 100) {
                    bridge.injectBridgeScript();
                    browser.evaluate("try { onBridgeInit(); } catch (ignored) { }");

                    new AsyncTask(() -> {
                        getLifeCycleListener().onNavigate(getCurrentURL());
                    });
                }
            }

            @Override
            public void completed(ProgressEvent event) {
                this.changed(event);
            }
        });

        this.browser.addTitleListener(new TitleListener() {
            @Override
            public void changed(TitleEvent event) {
                String title = event.title;

                if ((title == null) ||
                    title.equals("null") ||
                    title.equals("undefined") ||
                    title.isEmpty() ||
                    title.equals(getCurrentURL())) {
                    pageTitle = null;
                } else {
                    pageTitle = title;
                }

                new AsyncTask(() -> {
                    getLifeCycleListener().onPageTitleChange(pageTitle);
                });

                updateTitle();
            }
        });

        this.shell.setMinimumSize(this.windowState.getMinHeight(), this.windowState.getMinHeight());
        this.shell.setBounds(this.windowState.getX(), this.windowState.getY(), this.windowState.getWidth(), this.windowState.getHeight());

        this.shell.addControlListener(new ControlListener() {
            @Override
            public void controlMoved(ControlEvent e) {
                Point loc = shell.getLocation();

                windowState.setX(loc.x);
                windowState.setY(loc.y);
            }

            @Override
            public void controlResized(ControlEvent e) {
                Point size = shell.getSize();

                windowState.setWidth(size.x);
                windowState.setHeight(size.y);
            }
        });

        this.shell.addListener(SWT.Close, (event) -> {
            event.doit = false;
            this.getLifeCycleListener().onCloseRequested();
        });

        if (display.getMenuBar() != null) {
            display.getMenuBar().addListener(SWT.Activate, (event) -> {
                event.doit = true;
                this.getLifeCycleListener().onOpenRequested();
            });
        }

        new AsyncTask(() -> {
            // The bridge query code.
            // Note that AsyncTask will not hold the JVM open, so we can safely use it
            // without a shutdown mechanism.
            while (!this.shell.isDisposed()) {
                String result = (String) this.eval("return window.Bridge?.internal__wkGetQueryQueue();");

                if (result != null) {
                    try {
                        JsonArray arr = Rson.DEFAULT.fromJson(result, JsonArray.class);

                        for (JsonElement e : arr) {
                            bridge.query(e.getAsString());
                        }
                    } catch (JsonParseException e) {
                        FastLogger.logException(e);
                    }
                }
                try {
                    TimeUnit.MILLISECONDS.sleep(300);
                } catch (InterruptedException e) {}
            }
        });
    }

    @Override
    public void loadURL(@Nullable String _url) {
        MainThread.submitTask(() -> {
            String url = _url; // Pointer.

            if (url == null) {
                url = "about:blank";
            }

            this.browser.setUrl(url);
        });
    }

    @Override
    public String getCurrentURL() {
        return new MainThreadPromise<String>(() -> this.browser.getUrl()).await();
    }

    @SneakyThrows
    @Override
    public void executeJavaScript(@NonNull String script) {
        if (this.browser != null) {
            MainThread.submitTaskAndWait(() -> {
                this.browser.execute(script);
            });
        }
    }

    private Object eval(String line) {
        return new MainThreadPromise<Object>(() -> this.browser.evaluate(line, true)).await();
    }

    @Override
    public WebviewBridge getBridge() {
        return this.bridge;
    }

    @Override
    public void open(@Nullable String url) {
        if (!this.hasPreloaded) {
            this.hasPreloaded = true;
            // The following code initializes stuff related to AWT, which can't be done on
            // the main thread (it'll lock up). So we delegate it to another thread.
            MainThread.executeOffOfMainThread(() -> {
                this.getLifeCycleListener().onBrowserPreLoad();
            });
        }

        this.getLifeCycleListener().onBrowserOpen();

        MainThread.submitTask(() -> {
            this.mt_initialize();
//            this.shell.pack();
            this.shell.open();
            this.shell.setVisible(true);
            this.shell.setEnabled(true);
            this.shell.setActive();

            this.loadURL(url);
        });
    }

    @Override
    public void close() {
        MainThread.submitTask(() -> {
            // We destroy the shell to prevent it from sticking in the Dock.
            this.shell.setVisible(false);
            this.browser.setUrl("about:blank");
        });

        this.getLifeCycleListener().onBrowserClose();
    }

    @Override
    public void destroy() {
        MainThread.submitTask(() -> {
            this.shell.setVisible(false);
            this.shell.dispose();
        });
    }

    @SneakyThrows
    public void changeImage(URL icon) {
        if (icon != null) {
            try (InputStream in = icon.openStream()) {
                Image image = new Image(display, in);

                MainThread.submitTask(() -> {
                    this.shell.setImage(image);
                });
            }
        }
    }

    public void updateTitle() {
        new AsyncTask(() -> {
            String title;

            if (this.pageTitle != null) {
                title = this.pageTitle;
            } else {
                title = App.getName();
            }

            MainThread.submitTask(() -> {
                this.shell.setText(title);
            });
        });
    }

    @Override
    public void focus() {
        MainThread.submitTask(() -> {
            this.shell.setActive();
        });
    }

    @Override
    public boolean isOpen() {
        return new MainThreadPromise<>(() -> {
            return this.shell.isVisible();
        }).await();
    }

    @Override
    public void reload() {
        MainThread.submitTask(() -> {
            this.browser.refresh();
        });
    }

    @Override
    public void setPosition(int x, int y) {
        MainThread.submitTask(() -> {
            this.shell.setLocation(x, y);
        });
    }

    @Override
    public void setSize(int width, int height) {
        MainThread.submitTask(() -> {
            this.shell.setSize(width, height);
        });
    }

    @Override
    public void setProperties(@NonNull WebviewWindowProperties properties) {
        // TODO ugh, swt again.
    }

}
