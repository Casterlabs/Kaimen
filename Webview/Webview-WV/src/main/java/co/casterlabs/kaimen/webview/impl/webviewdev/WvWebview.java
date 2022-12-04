package co.casterlabs.kaimen.webview.impl.webviewdev;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import co.casterlabs.commons.platform.Arch;
import co.casterlabs.commons.platform.OSDistribution;
import co.casterlabs.kaimen.webview.Webview;
import co.casterlabs.kaimen.webview.WebviewFactory;
import co.casterlabs.kaimen.webview.WebviewRenderer;
import co.casterlabs.kaimen.webview.WebviewWindowProperties;
import co.casterlabs.kaimen.webview.bridge.WebviewBridge;
import lombok.Getter;
import lombok.NonNull;

public class WvWebview extends Webview {

    public static final WebviewFactory FACTORY = new WebviewFactory() {

        @Override
        public @Nullable Webview get() {
            return new WvWebview();
        }

        @Override
        public Map<OSDistribution, List<Arch>> getSupportMap() {
            Map<OSDistribution, List<Arch>> supported = new HashMap<>();

            supported.put(
                OSDistribution.WINDOWS_NT,
                Arrays.asList(Arch.AARCH64, Arch.X86_64, Arch.X86)
            );

            return supported;
        };

        @Override
        public WebviewRenderer getRendererType() {
            return WebviewRenderer.WEBVIEW_DEV;
        }

        @Override
        public boolean supportsTransparency() {
            return false;
        };

    };

    dev.webview.Webview webview;
    WvBridge bridge;

    private @Getter boolean isOpen = false;

    @Override
    protected void initialize0() throws Exception {
        this.webview = new dev.webview.Webview(true);
        this.bridge = new WvBridge(this);
    }

    @Override
    public void loadURL(@Nullable String url) {
        this.webview.loadURL(url);
    }

    @Override
    public String getCurrentURL() {
        // TODO
        return null;
    }

    @Override
    public @Nullable String getPageTitle() {
        // TODO
        return null;
    }

    @Override
    public void executeJavaScript(@NonNull String script) {
        if (!this.isOpen) return;
//        this.webview.eval(script);
    }

    @Override
    public WebviewBridge getBridge() {
        return this.bridge;
    }

    @Override
    public void open(@Nullable String url) {
        this.isOpen = true;
        this.loadURL(url);

        this.webview.run();
    }

    @Override
    public void close() {
        this.isOpen = false;
        this.webview.close();
    }

    @Override
    public void destroy() {
        this.close();
    }

    @Override
    public void focus() {
        // TODO
    }

    @Override
    public void reload() {
        this.executeJavaScript("location.reload();");
    }

    @Override
    public void setPosition(int x, int y) {
        // TODO
    }

    @Override
    public void setSize(int width, int height) {
        this.webview.setSize(width, height);
    }

    @Override
    public void setProperties(@NonNull WebviewWindowProperties properties) {
        // TODO
    }

    @Override
    public WebviewRenderer getRendererType() {
        return WebviewRenderer.WEBVIEW_DEV;
    }

}
