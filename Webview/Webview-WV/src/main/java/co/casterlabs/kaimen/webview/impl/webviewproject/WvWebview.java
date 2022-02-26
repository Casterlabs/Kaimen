package co.casterlabs.kaimen.webview.impl.webviewproject;

import org.jetbrains.annotations.Nullable;

import co.casterlabs.kaimen.webview.Webview;
import co.casterlabs.kaimen.webview.WebviewFactory;
import co.casterlabs.kaimen.webview.WebviewRenderer;
import co.casterlabs.kaimen.webview.WebviewWindowProperties;
import co.casterlabs.kaimen.webview.bridge.WebviewBridge;
import lombok.NonNull;

public class WvWebview extends Webview {

    public static final WebviewFactory FACTORY = new WebviewFactory() {

        @Override
        public @Nullable Webview produce() throws Exception {
            return new WvWebview();
        }

        @Override
        public WebviewRenderer getRendererType() {
            return WebviewRenderer.WEBVIEW_PROJECT;
        }

        @Override
        public boolean supportsTransparency() {
            return false;
        };

    };

    @Override
    protected void initialize0() throws Exception {
        // TODO Auto-generated method stub

    }

    @Override
    public void loadURL(@Nullable String url) {
        // TODO Auto-generated method stub

    }

    @Override
    public String getCurrentURL() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public @Nullable String getPageTitle() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void executeJavaScript(@NonNull String script) {
        // TODO Auto-generated method stub

    }

    @Override
    public WebviewBridge getBridge() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void open(@Nullable String url) {
        // TODO Auto-generated method stub

    }

    @Override
    public void close() {
        // TODO Auto-generated method stub

    }

    @Override
    public void destroy() {
        // TODO Auto-generated method stub

    }

    @Override
    public void focus() {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean isOpen() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void reload() {
        // TODO Auto-generated method stub

    }

    @Override
    public void setPosition(int x, int y) {
        // TODO Auto-generated method stub

    }

    @Override
    public void setSize(int width, int height) {
        // TODO Auto-generated method stub

    }

    @Override
    public void setProperties(@NonNull WebviewWindowProperties properties) {
        // TODO Auto-generated method stub

    }

}
