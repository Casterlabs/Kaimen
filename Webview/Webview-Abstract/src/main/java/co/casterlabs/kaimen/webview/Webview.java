package co.casterlabs.kaimen.webview;

import java.lang.ref.WeakReference;
import java.util.LinkedList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import co.casterlabs.kaimen.webview.bridge.WebviewBridge;
import co.casterlabs.kaimen.webview.scheme.SchemeHandler;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

public abstract class Webview {
    public static final String WEBVIEW_SCHEME = "app";
    public static final boolean isDev = false;

    protected static List<WeakReference<Webview>> webviews = new LinkedList<>();

    private @Getter boolean offScreenRenderingEnabled = false;
    private @Getter boolean transparencyEnabled = false;

    private @Getter @Setter SchemeHandler schemeHandler;

    private boolean initialized = false;

    private @Getter WebviewLifeCycleListener lifeCycleListener;
    protected @Getter WebviewWindowState windowState = new WebviewWindowState();

    private WeakReference<Webview> $ref = new WeakReference<>(this);

    public Webview() {
        webviews.add(this.$ref);
    }

    @Override
    protected void finalize() {
        webviews.remove(this.$ref);
    }

    public final void initialize(@Nullable WebviewLifeCycleListener lifeCycleListener, @Nullable WebviewWindowState windowState, boolean offScreenRenderingEnabled, boolean transparencyEnabled) throws Exception {
        assert !this.initialized : "Webview is already initialized.";

        if (windowState != null) {
            this.windowState = windowState;
        }

        if (lifeCycleListener == null) {
            this.lifeCycleListener = new WebviewLifeCycleListener() {
            };
        } else {
            this.lifeCycleListener = lifeCycleListener;
        }

        this.offScreenRenderingEnabled = offScreenRenderingEnabled;
        this.transparencyEnabled = transparencyEnabled;

        this.initialized = true;
        this.initialize0();
    }

    protected abstract void initialize0() throws Exception;

    public abstract void loadURL(@Nullable String url);

    public abstract String getCurrentURL();

    public abstract @Nullable String getPageTitle();

    public abstract void executeJavaScript(@NonNull String script);

    public abstract WebviewBridge getBridge();

    public abstract void open(@Nullable String url);

    public abstract void close();

    public abstract void destroy();

    public abstract void focus();

    public abstract boolean isOpen();

}
