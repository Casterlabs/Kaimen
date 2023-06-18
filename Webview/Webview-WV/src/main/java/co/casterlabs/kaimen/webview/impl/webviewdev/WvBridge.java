package co.casterlabs.kaimen.webview.impl.webviewdev;

import java.io.IOException;

import co.casterlabs.commons.async.AsyncTask;
import co.casterlabs.kaimen.webview.WebviewFileUtil;
import co.casterlabs.kaimen.webview.bridge.WebviewBridge;
import co.casterlabs.rakurai.json.element.JsonArray;
import co.casterlabs.rakurai.json.element.JsonElement;
import co.casterlabs.rakurai.json.element.JsonObject;
import co.casterlabs.rakurai.json.element.JsonString;
import lombok.NonNull;
import xyz.e3ndr.fastloggingframework.LogUtil;
import xyz.e3ndr.fastloggingframework.logging.FastLogger;
import xyz.e3ndr.fastloggingframework.logging.LogLevel;

public class WvBridge extends WebviewBridge {
    private static String bridgeScript = "";

    private WvWebview webview;

    static {
        // Get the javascript bridge.
        try {
            bridgeScript = WebviewFileUtil.loadResourceFromBuildProject("WV_JavascriptBridge.js", "Webview-WV");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public WvBridge(WvWebview webview) {
        this.webview = webview;
        this.defineObject("windowState", this.webview.getWindowState());

        this.webview.webview.setInitScript(bridgeScript);

        this.webview.webview.bind("wvInit", (arguments) -> {
            this.init();
            return null;
        });

        this.webview.webview.bind("wvPoke", (arguments) -> {
            handleEmission(arguments.getObject(0));
            return null;
        });
    }

    @Override
    protected void emit0(@NonNull String type, @NonNull JsonElement data) {
        AsyncTask.create(() -> {
            this.webview.executeJavaScript(String.format("window.Bridge.broadcast(%s,%s);", new JsonString(type), data));
        });
    }

    @Override
    protected void eval0(@NonNull String script) {
        this.webview.executeJavaScript(script);
    }

    @Override
    protected void invokeCallback0(@NonNull String invokeId, @NonNull JsonArray arguments) {
        this.emit("callback:" + invokeId, arguments);
    }

    @Override
    protected void removeCallback0(@NonNull String invokeId) {
        this.emit("callback:" + invokeId + ":remove", null);
    }

    @Override
    protected String getNativeBridgeScript() {
        return bridgeScript;
    }

    private void handleEmission(@NonNull JsonObject query) {
        JsonObject emission = query.getObject("data");
        String type = emission.getString("type");
        JsonObject data = emission.getObject("data");

        FastLogger.logStatic(LogLevel.TRACE, "%s: %s", type, data);

        if (type.startsWith("_get:")) {
            String id = type.substring("_get:".length());
            String property = data.getString("property");
            String nonce = data.getString("nonce");

            try {
                JsonElement result = this.processGet(id, property);

                emit(
                    "_get:" + nonce,
                    new JsonObject()
                        .put("isError", false)
                        .put("result", result)
                );
            } catch (Throwable e) {
                String error = LogUtil.getExceptionStack(e);

                emit(
                    "_get:" + nonce,
                    new JsonObject()
                        .put("isError", true)
                        .put("result", error)
                );
            }
        } else if (type.startsWith("_set:")) {
            String id = type.substring("_set:".length());
            String property = data.getString("property");
            JsonElement value = data.get("value");

            try {
                this.processSet(id, property, value);
            } catch (Throwable e) {
                FastLogger.logException(e);
            }
        } else if (type.startsWith("_invoke:")) {
            String id = type.substring("_invoke:".length());
            String function = data.getString("function");
            JsonArray args = data.getArray("args");
            String nonce = data.getString("nonce");

            try {
                JsonElement result = this.processInvoke(id, function, args);

                emit(
                    "_invoke:" + nonce,
                    new JsonObject()
                        .put("isError", false)
                        .put("result", result)
                );
            } catch (Throwable e) {
                String error = LogUtil.getExceptionStack(e);

                emit(
                    "_invoke:" + nonce,
                    new JsonObject()
                        .put("isError", true)
                        .put("result", error)
                );
            }
        } else if (this.onEvent != null) {
            this.onEvent.accept(type, data);
        }
    }

}
