package co.casterlabs.kaimen.webview.impl.cef;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Window;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.WindowConstants;

import org.cef.CefClient;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.callback.CefContextMenuParams;
import org.cef.callback.CefMenuModel;
import org.cef.handler.CefContextMenuHandlerAdapter;
import org.cef.handler.CefDisplayHandlerAdapter;
import org.cef.handler.CefLifeSpanHandlerAdapter;
import org.cef.handler.CefLoadHandlerAdapter;
import org.cef.network.CefRequest.TransitionType;
import org.jetbrains.annotations.Nullable;

import co.casterlabs.commons.async.AsyncTask;
import co.casterlabs.commons.platform.ArchFamily;
import co.casterlabs.commons.platform.OSDistribution;
import co.casterlabs.kaimen.app.App;
import co.casterlabs.kaimen.webview.Webview;
import co.casterlabs.kaimen.webview.WebviewFactory;
import co.casterlabs.kaimen.webview.WebviewRenderer;
import co.casterlabs.kaimen.webview.WebviewWindowProperties;
import co.casterlabs.kaimen.webview.bridge.WebviewBridge;
import lombok.Getter;
import lombok.NonNull;
import xyz.e3ndr.fastloggingframework.logging.FastLogger;

public class CefWebview extends Webview {

    public static final WebviewFactory FACTORY = new WebviewFactory() {

        @Override
        public @Nullable Webview get() {
            return new CefWebview();
        }

        @Override
        public Map<OSDistribution, List<String>> getSupportMap() {
            return Map.of(
                OSDistribution.LINUX, Arrays.asList(
                    ArchFamily.ARM.getArchTarget(64),
                    ArchFamily.X86.getArchTarget(64),
                    ArchFamily.X86.getArchTarget(32)
                ),

                OSDistribution.WINDOWS_NT, Arrays.asList(
                    ArchFamily.ARM.getArchTarget(64),
                    ArchFamily.X86.getArchTarget(64),
                    ArchFamily.X86.getArchTarget(32)
                )
            );
        };

        @Override
        public WebviewRenderer getRendererType() {
            return WebviewRenderer.CHROMIUM_EMBEDDED_FRAMEWORK;
        }

    };

    private static FastLogger logger = new FastLogger();
    private static boolean cefInitialized = false;

    private @Getter Window window;
    private JPanel cefPanel;

    private CefClient client;
    private CefJavascriptBridge bridge;

    private CefBrowser browser;
    private CefDevTools devtools;

    private @Getter String pageTitle;

    @SuppressWarnings("deprecation")
    @Override
    protected void initialize0() throws Exception {
        // One-time setup.
        if (!cefInitialized) {
            cefInitialized = true;
            CefUtil.create(false /* I hate this. */);
        }

        // Setup the panel
        this.cefPanel = new JPanel();
        this.cefPanel.setLayout(new BorderLayout(0, 0));

        // Create the window.
        if (this.isTransparencyEnabled()) {
            JDialog dialog = new JDialog((Window) null);

            dialog.setUndecorated(true);
            dialog.getRootPane().setOpaque(false);
            dialog.getContentPane().setBackground(new Color(0, true));
            this.cefPanel.setOpaque(false);
            this.cefPanel.setBackground(new Color(0, true));

            this.window = dialog;
        } else {
            JFrame frame = new JFrame();

            frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
            frame.setResizable(true);
            frame.setMinimumSize(new Dimension(this.windowState.getMinWidth(), this.windowState.getMinHeight()));

            this.window = frame;
        }

        EventQueue.invokeAndWait(this.window::addNotify);
        App.shakeWindowProperties(this.window);

        if (App.getIconURL() != null) {
            this.window.setIconImage(new ImageIcon(App.getIconURL()).getImage());
        }

        this.window.setSize(this.windowState.getWidth(), this.windowState.getHeight());
        this.window.setLocation(this.windowState.getX(), this.windowState.getY());

        this.window.add(this.cefPanel, BorderLayout.CENTER);

        this.window.addWindowFocusListener(new WindowAdapter() {
            @Override
            public void windowGainedFocus(WindowEvent e) {
                CefWebview.this.windowState.setHasFocus(true);
            }

            @Override
            public void windowLostFocus(WindowEvent e) {
                CefWebview.this.windowState.setHasFocus(false);
            }
        });

        this.window.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                if (!isMaximized()) {
                    CefWebview.this.windowState.setWidth(window.getWidth());
                    CefWebview.this.windowState.setHeight(window.getHeight());
                }
            }

            @Override
            public void componentMoved(ComponentEvent e) {
                if (!isMaximized()) {
                    CefWebview.this.windowState.setX(window.getX());
                    CefWebview.this.windowState.setY(window.getY());
                }
            }
        });

        this.window.addWindowListener(new WindowAdapter() {
            @Override
            public void windowIconified(WindowEvent e) {
                CefWebview.this.getLifeCycleListener().onMinimize();
            }

            @Override
            public void windowClosing(WindowEvent e) {
                CefWebview.this.getLifeCycleListener().onCloseRequested();
            }
        });

        // Cef
        this.client = CefUtil.createCefClient();
        this.bridge = new CefJavascriptBridge(this.client);

        // Context menu
        this.client.addContextMenuHandler(new CefContextMenuHandlerAdapter() {
            // ID | Name
            // ---+-------------------------
            // 01 | Inspect Element
            // 02 | Reload
            //

            @Override
            public void onBeforeContextMenu(CefBrowser browser, CefFrame frame, CefContextMenuParams params, CefMenuModel model) {
                model.clear();

//                if ( Bootstrap.isDev() || Bootstrap.getInstance().isDevToolsEnabled()) {
                model.addItem(2, "Reload");

                model.addCheckItem(1, "Inspect Element");
                model.setChecked(1, devtools.isOpen());

//                    model.addSeparator();
//                    model.addItem(99, "Close This Popup");
//                }
            }

            @Override
            public boolean onContextMenuCommand(CefBrowser browser, CefFrame frame, CefContextMenuParams params, int commandId, int eventFlags) {
                switch (commandId) {
                    case 1: {
                        devtools.toggle();
                        break;
                    }

                    case 2: {
                        executeJavaScript("location.reload(true);");
                        break;
                    }
                }
                return true;
            }
        });

        // Load handler
        logger.debug("Loadstate 0");
        this.getLifeCycleListener().onBrowserPreLoad();
        this.client.addLoadHandler(new CefLoadHandlerAdapter() {

            @Override
            public void onLoadEnd(CefBrowser _browser, CefFrame _frame, int httpStatusCode) {
                executeJavaScript("try { onBridgeInit(); } catch (ignored) { }");

                AsyncTask.create(() -> {
                    getLifeCycleListener().onNavigate(getCurrentURL());
                });
            }

            @Override
            public void onLoadStart(CefBrowser _browser, CefFrame _frame, TransitionType transitionType) {
                if (browser == _browser) {
                    logger.info("Injected Bridge.");
                    bridge.defineObject("windowState", windowState);
                    bridge.injectBridgeScript(browser.getMainFrame());
                }
            }
        });

        // Lifespan
        this.client.addLifeSpanHandler(new CefLifeSpanHandlerAdapter() {
            @Override
            public void onAfterCreated(CefBrowser _browser) {
                if (browser == _browser) {
                    logger.info("Created window.");
                    devtools = new CefDevTools(browser);
                }
            }
        });

        this.client.addDisplayHandler(new CefDisplayHandlerAdapter() {
            @Override
            public void onTitleChange(CefBrowser browser, String title) {
                if ((title == null) ||
                    title.equals("null") ||
                    title.equals("undefined") ||
                    title.isEmpty() ||
                    getCurrentURL().contains(title)) {
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

        App.setAppearance(App.getAppearance()); // Trigger the set.
    }

    public void updateTitle() {
        if (!this.isTransparencyEnabled()) {
            AsyncTask.create(() -> {
                String title;

                if (this.pageTitle != null) {
                    title = this.pageTitle;
                } else {
                    title = App.getName();
                }

                ((JFrame) this.window).setTitle(title);
            });
        }
    }

    public boolean isMaximized() {
        if (this.isTransparencyEnabled()) {
            return false;
        } else {
            return (((JFrame) this.window).getExtendedState() & JFrame.MAXIMIZED_BOTH) == JFrame.MAXIMIZED_BOTH;
        }
    }

    @Override
    public void loadURL(@Nullable String url) {
        if (url == null) {
            url = "about:blank";
        }

        this.browser.loadURL(url);
    }

    @Override
    public String getCurrentURL() {
        return this.browser.getURL();
    }

    @Override
    public void executeJavaScript(@NonNull String script) {
        this.browser.executeJavaScript(script, "app://app.local", 0);
    }

    @Override
    public WebviewBridge getBridge() {
        return this.bridge;
    }

    @Override
    public void open(@Nullable String url) {
        if (this.browser == null) {
            if (url == null) {
                url = "about:blank";
            }

            // Create browser
            this.browser = this.client.createBrowser(
                url,
                this.isOffScreenRenderingEnabled(),
                this.isTransparencyEnabled()
            );

            // Add it to the JPanel.
            this.cefPanel.add(this.browser.getUIComponent(), BorderLayout.CENTER);
            this.window.setVisible(true);

            // Notify
            this.getLifeCycleListener().onBrowserOpen();
        }
    }

    @Override
    public void close() {
        if (this.browser != null) {
            this.window.setVisible(false);

            // Remove the frame
            this.cefPanel.removeAll();

            // Destroy the browser and devtools
            if (this.devtools != null) {
                this.devtools.close();
            }

            browser.close(false);
            browser = null;

            // Notify
            this.getLifeCycleListener().onBrowserClose();
        }
    }

    @Override
    public void destroy() {
        this.window.dispose();
    }

    @Override
    public void focus() {
        this.window.toFront();
    }

    @Override
    public boolean isOpen() {
        return this.window.isVisible();
    }

    @Override
    public void reload() {
        this.browser.reloadIgnoreCache();
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

    @Override
    public WebviewRenderer getRendererType() {
        return WebviewRenderer.CHROMIUM_EMBEDDED_FRAMEWORK;
    }

}
