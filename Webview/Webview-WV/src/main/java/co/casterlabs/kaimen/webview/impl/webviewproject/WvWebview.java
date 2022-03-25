package co.casterlabs.kaimen.webview.impl.webviewproject;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import co.casterlabs.kaimen.util.platform.Arch;
import co.casterlabs.kaimen.util.platform.OperatingSystem;
import co.casterlabs.kaimen.util.threading.AsyncTask;
import co.casterlabs.kaimen.webview.Webview;
import co.casterlabs.kaimen.webview.WebviewFactory;
import co.casterlabs.kaimen.webview.WebviewRenderer;
import co.casterlabs.kaimen.webview.WebviewWindowProperties;
import co.casterlabs.kaimen.webview.bridge.WebviewBridge;
import co.casterlabs.rakurai.json.element.JsonArray;
import co.casterlabs.rakurai.json.element.JsonElement;
import co.casterlabs.rakurai.json.element.JsonObject;
import lombok.NonNull;

public class WvWebview extends Webview {

    public static final WebviewFactory FACTORY = new WebviewFactory() {

        @Override
        public @Nullable Webview produce() throws Exception {
            return new WvWebview();
        }

        @Override
        public Map<OperatingSystem, List<Arch>> getSupportMap() {
            Map<OperatingSystem, List<Arch>> supported = new HashMap<>();

            for (OperatingSystem os : OperatingSystem.values()) {
                supported.put(os, Arrays.asList(Arch.AMD64));
            }

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

    private dev.webview.Webview wv;
    private WvBridge bridge = new WvBridge(this);

    @Override
    protected void initialize0() throws Exception {}

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
        this.wv.eval(script);
    }

    @Override
    public WebviewBridge getBridge() {
        return this.bridge;
    }

    @Override
    public void open(@Nullable String url) {
        this.wv = new dev.webview.Webview();

        this.wv.setInitScript(this.bridge.getInitScript());

        this.wv.bind("__internal_comms", (JsonArray args) -> {
            JsonElement e = args.get(0);

            if (e.isJsonObject()) {
                JsonObject data = e.getAsObject();

                this.bridge.handleEmission(data);
            } else {
                String value = e.getAsString();

                switch (value) {
                    case "INIT": {
                        this.bridge.initNoInject();
                        break;
                    }
                }
            }
            return null;
        });

        this.wv.loadURL(url);

        new AsyncTask(this.wv::run);
    }

    @Override
    public void close() {
        this.wv.close();
    }

    @Override
    public void destroy() {
        this.close();
    }

    @Override
    public void focus() {}

    @Override
    public boolean isOpen() {
        return this.wv != null;
    }

    @Override
    public void reload() {
        this.executeJavaScript("location.reload();");
    }

    @Override
    public void setPosition(int x, int y) {}

    @Override
    public void setSize(int width, int height) {
        this.wv.setSize(width, height);
    }

    @Override
    public void setProperties(@NonNull WebviewWindowProperties properties) {}

}
