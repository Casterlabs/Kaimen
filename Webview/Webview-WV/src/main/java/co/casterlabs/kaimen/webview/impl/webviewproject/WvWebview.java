package co.casterlabs.kaimen.webview.impl.webviewproject;

import java.awt.BorderLayout;
import java.awt.Canvas;
import java.awt.Dimension;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.WindowConstants;

import org.jetbrains.annotations.Nullable;

import co.casterlabs.kaimen.app.App;
import co.casterlabs.kaimen.util.platform.Arch;
import co.casterlabs.kaimen.util.platform.OperatingSystem;
import co.casterlabs.kaimen.util.platform.Platform;
import co.casterlabs.kaimen.util.threading.AsyncTask;
import co.casterlabs.kaimen.util.threading.MainThread;
import co.casterlabs.kaimen.webview.Webview;
import co.casterlabs.kaimen.webview.WebviewFactory;
import co.casterlabs.kaimen.webview.WebviewRenderer;
import co.casterlabs.kaimen.webview.WebviewWindowProperties;
import co.casterlabs.kaimen.webview.bridge.WebviewBridge;
import co.casterlabs.rakurai.json.element.JsonArray;
import co.casterlabs.rakurai.json.element.JsonElement;
import co.casterlabs.rakurai.json.element.JsonObject;
import lombok.NonNull;
import lombok.SneakyThrows;

public class WvWebview extends Webview {

    public static final WebviewFactory FACTORY = new WebviewFactory() {

        @Override
        public @Nullable Webview produce() throws Exception {
            return new WvWebview();
        }

        @Override
        public Map<OperatingSystem, List<Arch>> getSupportMap() {
            Map<OperatingSystem, List<Arch>> supported = new HashMap<>();

//            for (OperatingSystem os : OperatingSystem.values()) {
//                supported.put(os, Arrays.asList(Arch.AMD64));
//            }

            supported.put(
                OperatingSystem.WINDOWS,
                Arrays.asList(Arch.X86, Arch.AMD64)
            ); // Both x86 and amd64 are supported on Windows.

            return supported;
        };

        @Override
        public WebviewRenderer getRendererType() {
            return WebviewRenderer.WEBVIEW_PROJECT;
        }

        @Override
        public boolean supportsTransparency() {
            return false;
        }

    };

    private JFrame window;
    private Canvas wvCanvas;

    private dev.webview.Webview wv;
    private WvBridge bridge = new WvBridge(this);

    @Override
    protected void initialize0() throws Exception {
        // Setup the canvas
        this.wvCanvas = new Canvas();

        // Create the window.
        this.window = new JFrame();

        this.window.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        this.window.setResizable(true);
        this.window.setMinimumSize(new Dimension(this.windowState.getMinWidth(), this.windowState.getMinHeight()));

        if (App.getIconURL() != null) {
            this.window.setIconImage(new ImageIcon(App.getIconURL()).getImage());
        }

        this.window.setSize(this.windowState.getWidth(), this.windowState.getHeight());
        this.window.setLocation(this.windowState.getX(), this.windowState.getY());

        this.window.add(this.wvCanvas, BorderLayout.CENTER);

        // Listeners galore.
        this.window.addWindowFocusListener(new WindowAdapter() {
            @Override
            public void windowGainedFocus(WindowEvent e) {
                windowState.setHasFocus(true);
            }

            @Override
            public void windowLostFocus(WindowEvent e) {
                windowState.setHasFocus(false);
            }
        });

        this.window.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                int width = window.getWidth();
                int height = window.getHeight();

                if (!isMaximized()) {
                    windowState.setWidth(width);
                    windowState.setHeight(height);
                }

                updateWebviewSize(width, height);
            }

            @Override
            public void componentMoved(ComponentEvent e) {
                if (!isMaximized()) {
                    windowState.setX(window.getX());
                    windowState.setY(window.getY());
                }
            }
        });

        this.window.addWindowListener(new WindowAdapter() {
            @Override
            public void windowIconified(WindowEvent e) {
                getLifeCycleListener().onMinimize();
            }

            @Override
            public void windowClosing(WindowEvent e) {
                getLifeCycleListener().onCloseRequested();
            }
        });

        this.bridge.defineObject("windowState", this.windowState);

        this.getLifeCycleListener().onBrowserPreLoad();
    }

    private void updateWebviewSize(int width, int height) {
        if (this.wv != null) {
            // There is a random margin on Windows, so we must compensate.
            // TODO figure out what this is caused by.
            if (Platform.os == OperatingSystem.WINDOWS) {
                width -= 32;
                height -= 78;
            }

            this.wv.setSize(width, height);
            this.wv.setFixedSize(width, height);
        }
    }

    private boolean isMaximized() {
        return (this.window.getExtendedState() & JFrame.MAXIMIZED_BOTH) == JFrame.MAXIMIZED_BOTH;
    }

    @Override
    public void loadURL(@Nullable String url) {
        this.wv.loadURL(url);
    }

    @Override
    public String getCurrentURL() {
        return "about:blank";
    }

    @Override
    public @Nullable String getPageTitle() {
        return null;
    }

    @Override
    public void executeJavaScript(@NonNull String script) {
        if (this.wv != null) {
            this.wv.dispatch(() -> {
                this.wv.eval(script);
            });
        }
    }

    @Override
    public WebviewBridge getBridge() {
        return this.bridge;
    }

    @SuppressWarnings("deprecation")
    @SneakyThrows
    @Override
    public void open(@Nullable String url) {
        this.window.setVisible(true);

        MainThread.submitTaskAndWait(() -> {
            this.wv = new dev.webview.Webview(true, this.wvCanvas);

            this.wv.bind("__internal_comms", (JsonArray args) -> {
                try {
                    JsonElement e = args.get(0);

                    if (e.isJsonObject()) {
                        JsonObject data = e.getAsObject();

                        this.bridge.handleEmission(data);
                    } else {
                        String value = e.getAsString();

                        switch (value) {
                            case "INIT": {
                                this.bridge.initNoInject();
                                this.executeJavaScript("try { onBridgeInit(); } catch (ignored) { }");
                                new AsyncTask(this.getLifeCycleListener()::onBrowserOpen);
                                break;
                            }
                        }
                    }
                } catch (Throwable t) {
                    t.printStackTrace();
                }

                return null;
            });

            this.wv.setInitScript(this.bridge.getInitScript());

            this.wv.loadURL(url);

            this.updateWebviewSize(this.window.getWidth(), this.window.getHeight());
        });

        MainThread.submitTask(() -> {
            MainThread.setImpl(this.wv::dispatch);
            this.wv.run();

            // The impl is automatically unregistered once this exits.
        });
    }

    @Override
    public boolean isOpen() {
        return this.window.isVisible();
    }

    @Override
    public void close() {
        this.wv.close();
        this.window.setVisible(false);
        this.getLifeCycleListener().onBrowserClose();
    }

    @Override
    public void destroy() {
        this.close();
        this.window.dispose();
    }

    @Override
    public void focus() {
        this.window.toFront();
    }

    @Override
    public void reload() {
        this.executeJavaScript("location.reload(true);");
    }

    @Override
    public void setPosition(int x, int y) {
        this.window.setLocation(x, y);
    }

    @Override
    public void setSize(int width, int height) {
        this.window.setSize(width, height);
    }

    @Override
    public void setProperties(@NonNull WebviewWindowProperties properties) {
        this.window.setFocusable(properties.isFocusable());
        this.window.setAlwaysOnTop(properties.isAlwaysOnTop());
    }

}
