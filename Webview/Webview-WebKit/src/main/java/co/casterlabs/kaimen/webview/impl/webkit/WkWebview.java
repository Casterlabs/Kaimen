package co.casterlabs.kaimen.webview.impl.webkit;

import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.BrowserFunction;
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

import co.casterlabs.commons.async.AsyncTask;
import co.casterlabs.commons.platform.ArchFamily;
import co.casterlabs.commons.platform.OSDistribution;
import co.casterlabs.commons.platform.Platform;
import co.casterlabs.kaimen.app.App;
import co.casterlabs.kaimen.webview.Webview;
import co.casterlabs.kaimen.webview.WebviewFactory;
import co.casterlabs.kaimen.webview.WebviewRenderer;
import co.casterlabs.kaimen.webview.WebviewWindowProperties;
import co.casterlabs.kaimen.webview.bridge.WebviewBridge;
import lombok.Getter;
import lombok.NonNull;
import lombok.SneakyThrows;
import xyz.e3ndr.reflectionlib.ReflectionLib;

public class WkWebview extends Webview {
    private static Display display;

    public static final WebviewFactory FACTORY = new WebviewFactory() {

        @Override
        public @Nullable Webview get() {
            return new WkWebview();
        }

        @Override
        public Map<OSDistribution, List<String>> getSupportMap() {
            return Map.of(
                OSDistribution.MACOS, Arrays.asList(
                    ArchFamily.ARM.getArchTarget(64),
                    ArchFamily.X86.getArchTarget(64)
                )
            );
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

    private BrowserFunction func;
    private Browser browser;
    private Shell shell;

    private @Getter String pageTitle;

    @SneakyThrows
    @Override
    protected void initialize0() {
        if (this.isTransparencyEnabled()) {
            throw new UnsupportedOperationException("Transparency is not supported on macOS at this time.");
        }

        if (display == null) {
            App.getMainThread().executeWithPromise(() -> {
                display = Display.getCurrent();
                return null;
            }).await();
        }

        App.getMainThread().executeWithPromise(() -> {
            this.mt_initialize();
            return null;
        }).await();

        this.changeImage(App.getIconURL());
    }

    private void mt_initialize() {
        this.shell = new Shell(display, SWT.SHELL_TRIM);
        this.shell.setLayout(new FillLayout());

        this.browser = new Browser(this.shell, SWT.WEBKIT);

        if (this.func != null) {
            this.func.dispose();
        }

        this.func = new BrowserFunction(this.browser, "__wkinternal_ipc_send") {
            @Override
            public Object function(Object[] arguments) {
                bridge.query((String) arguments[0]);
                return null;
            }
        };

        this.browser.setUrl("about:blank");

        try {
            if (Platform.osDistribution == OSDistribution.MACOS) {
                Object webkit = ReflectionLib.getValue(browser, "webBrowser"); // org.eclipse.swt.browser.WebKit
                Object view = ReflectionLib.getValue(webkit, "webView"); // org.eclipse.swt.internal.cocoa.WebView

                Object ns_applicationName = ReflectionLib.invokeStaticMethod(
                    Class.forName("org.eclipse.swt.internal.cocoa.NSString"),
                    "stringWith",
                    String.format("Safari/522.0 Kaimen (%s)", Webview.getPassword())
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

                    AsyncTask.create(() -> {
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

                AsyncTask.create(() -> {
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
    }

    @Override
    public void loadURL(@Nullable String _url) {
        App.getMainThread().execute(() -> {
            String url = _url; // Pointer.

            if (url == null) {
                url = "about:blank";
            }

            this.browser.setUrl(url);
        });
    }

    @SneakyThrows
    @Override
    public String getCurrentURL() {
        return App.getMainThread().executeWithPromise(() -> this.browser.getUrl()).await();
    }

    @SneakyThrows
    @Override
    public void executeJavaScript(@NonNull String script) {
        if (this.browser != null) {
            App.getMainThread().executeWithPromise(() -> {
                this.browser.execute(script);
                return null;
            }).await();
        }
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
            App.getMainThread().executeOffOfMainThread(() -> {
                this.getLifeCycleListener().onBrowserPreLoad();
            });
        }

        this.getLifeCycleListener().onBrowserOpen();

        App.getMainThread().execute(() -> {
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
        App.getMainThread().execute(() -> {
            // We destroy the shell to prevent it from sticking in the Dock.
            this.shell.setVisible(false);
            this.browser.setUrl("about:blank");
        });

        this.getLifeCycleListener().onBrowserClose();
    }

    @Override
    public void destroy() {
        App.getMainThread().execute(() -> {
            this.shell.setVisible(false);
            this.func.dispose();
            this.shell.dispose();
        });
    }

    @SneakyThrows
    public void changeImage(URL icon) {
        if (icon != null) {
            try (InputStream in = icon.openStream()) {
                Image image = new Image(display, in);

                App.getMainThread().execute(() -> {
                    this.shell.setImage(image);
                });
            }
        }
    }

    public void updateTitle() {
        AsyncTask.create(() -> {
            String title;

            if (this.pageTitle != null) {
                title = this.pageTitle;
            } else {
                title = App.getName();
            }

            App.getMainThread().execute(() -> {
                this.shell.setText(title);
            });
        });
    }

    @Override
    public void focus() {
        App.getMainThread().execute(() -> {
            this.shell.setActive();
        });
    }

    @SneakyThrows
    @Override
    public boolean isOpen() {
        return App.getMainThread().executeWithPromise(() -> {
            return this.shell.isVisible();
        }).await();
    }

    @Override
    public void reload() {
        App.getMainThread().execute(() -> {
            this.browser.refresh();
        });
    }

    @Override
    public void setPosition(int x, int y) {
        App.getMainThread().execute(() -> {
            this.shell.setLocation(x, y);
        });
    }

    @Override
    public void setSize(int width, int height) {
        App.getMainThread().execute(() -> {
            this.shell.setSize(width, height);
        });
    }

    @Override
    public void setProperties(@NonNull WebviewWindowProperties properties) {
        // TODO ugh, swt again.
    }

    @Override
    public WebviewRenderer getRendererType() {
        return WebviewRenderer.WEBKIT;
    }

}
