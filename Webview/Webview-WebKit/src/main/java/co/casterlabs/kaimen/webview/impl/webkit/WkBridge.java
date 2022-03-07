package co.casterlabs.kaimen.webview.impl.webkit;

import java.io.IOException;

import co.casterlabs.kaimen.util.threading.AsyncTask;
import co.casterlabs.kaimen.webview.WebviewFileUtil;
import co.casterlabs.kaimen.webview.bridge.BridgeValue;
import co.casterlabs.kaimen.webview.bridge.WebviewBridge;
import co.casterlabs.rakurai.json.Rson;
import co.casterlabs.rakurai.json.element.JsonArray;
import co.casterlabs.rakurai.json.element.JsonElement;
import co.casterlabs.rakurai.json.element.JsonNull;
import co.casterlabs.rakurai.json.element.JsonObject;
import co.casterlabs.rakurai.json.element.JsonString;
import co.casterlabs.rakurai.json.serialization.JsonParseException;
import lombok.NonNull;
import xyz.e3ndr.fastloggingframework.logging.FastLogger;
import xyz.e3ndr.fastloggingframework.logging.LogLevel;
import xyz.e3ndr.fastloggingframework.logging.LoggingUtil;

public class WkBridge extends WebviewBridge {
    private static String bridgeScript = "";

    private WkWebview webview;

    static {
        // Get the javascript bridge.
        try {
            bridgeScript = WebviewFileUtil.loadResourceFromBuildProject("SWT_JavascriptBridge.js", "Webview-Webkit");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public WkBridge(WkWebview webview) {
        this.webview = webview;
    }

    @Override
    public void emit(@NonNull String type, @NonNull JsonElement data) {
        String script = String.format("window.Bridge.broadcast(%s,%s);", new JsonString(type), data);

//        if (!type.startsWith("querynonce:")) {
        FastLogger.logStatic(LogLevel.TRACE, "emission [%s]: %s", type, data);
//        }

        this.eval(script);
    }

    @Override
    public void eval(@NonNull String script) {
        webview.executeJavaScript(script);
    }

    @Override
    public void invokeCallback(@NonNull String invokeId, @NonNull JsonArray arguments) {
        this.emit("callback:" + invokeId, arguments);
    }

    public void injectBridgeScript() {
        this.eval(bridgeScript);
        this.init();

        // Lifecycle listener. (Outside of the main thread)
        new AsyncTask(() -> {
//            this.loadPromise.fulfill(null);
            this.attachValue(this.webview.getWindowState().getBridge());
        });
    }

    // Called by SwtWebview
    public void query(String request) {
        FastLogger.logStatic(LogLevel.TRACE, request);

        try {
            JsonObject query = Rson.DEFAULT.fromJson(request, JsonObject.class);

            switch (query.getString("type")) {
                case "emission": {
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
                            String error = LoggingUtil.getExceptionStack(e);

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
                            String error = LoggingUtil.getExceptionStack(e);

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
                    break;
                }

                case "query": {
                    String queryField = query.getString("field");
                    String queryNonce = query.getString("nonce");

                    BridgeValue<?> bv = this.getQueryData().get(queryField);
                    JsonElement el = JsonNull.INSTANCE;

                    if (bv != null) {
                        el = bv.getAsJson();
                    }

                    emit("querynonce:" + queryNonce, new JsonObject().put("data", el));
                    break;
                }
            }
        } catch (JsonParseException ignored) {}
    }

}
