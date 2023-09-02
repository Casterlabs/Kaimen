package co.casterlabs.kaimen.webview.impl.cef;

import java.io.IOException;
import java.net.URLDecoder;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.cef.CefClient;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.browser.CefMessageRouter;
import org.cef.callback.CefQueryCallback;
import org.cef.handler.CefMessageRouterHandlerAdapter;

import co.casterlabs.commons.async.AsyncTask;
import co.casterlabs.kaimen.webview.WebviewFileUtil;
import co.casterlabs.kaimen.webview.bridge.WebviewBridge;
import co.casterlabs.rakurai.json.Rson;
import co.casterlabs.rakurai.json.element.JsonArray;
import co.casterlabs.rakurai.json.element.JsonElement;
import co.casterlabs.rakurai.json.element.JsonObject;
import co.casterlabs.rakurai.json.element.JsonString;
import lombok.NonNull;
import xyz.e3ndr.fastloggingframework.LogUtil;
import xyz.e3ndr.fastloggingframework.logging.FastLogger;
import xyz.e3ndr.fastloggingframework.logging.LogLevel;

public class CefJavascriptBridge extends WebviewBridge {
    private static String bridgeScript = "";

    private CefMessageRouter router;
    private CefFrame frame;

    private ExecutorService threadPool = Executors.newCachedThreadPool();

    static {
        try {
            bridgeScript = WebviewFileUtil.loadResourceFromBuildProject("CEF_JavascriptBridge.js", "Webview-CEF");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public CefJavascriptBridge(@NonNull CefClient client) {
        this.router = CefMessageRouter.create();

        this.router.addHandler(new CefMessageRouterHandlerAdapter() {

            @Override
            public boolean onQuery(CefBrowser browser, CefFrame frame, long queryId, String _request, boolean persistent, CefQueryCallback callback) {
                threadPool.submit(() -> {
                    try {
                        // CEF has a weird internal way of handling strings, and unfortunately
                        // the JNI wrapper mangles them. We've opted to just use URI encoding
                        // to prevent manglage. decodeURIComponent implementation taken from here:
                        // https://stackoverflow.com/a/6926987/11611152
                        String request = URLDecoder.decode(_request.replace("+", "%2B"), "UTF-8")
                            .replace("%2B", "+");

                        JsonObject query = Rson.DEFAULT.fromJson(request, JsonObject.class);

                        switch (query.getString("type")) {
                            case "emission": {
                                if (!persistent) {
                                    handleEmission(query);
                                }
                                break;
                            }

                            default: {
                                callback.failure(-2, "Invalid payload type.");
                            }
                        }

                        callback.success("");
                    } catch (Exception e) {
                        e.printStackTrace();
                        callback.failure(-2, "Invalid JSON payload.");
                    }
                });

                return true;
            }

            @Override
            public void onQueryCanceled(CefBrowser browser, CefFrame frame, long queryId) {}

        }, true);

        client.addMessageRouter(this.router);
    }

    @Override
    protected void emit0(@NonNull String type, @NonNull JsonElement data) {
        AsyncTask.create(() -> {
            String line = String.format("window.Bridge.broadcast(%s,%s);", new JsonString(type), data);

            this.frame.executeJavaScript(line, "", 1);
        });
    }

    @Override
    protected void eval0(@NonNull String script) {
        if (this.frame != null) {
//            try {
//                this.loadPromise.await();
//            } catch (Throwable e) {}

            this.frame.executeJavaScript(script, "", 1);
        }
    }

    @Override
    protected void invokeCallback0(@NonNull String invokeId, @NonNull JsonArray arguments) {
        this.emit("callback:" + invokeId, arguments);
    }

    @Override
    protected void removeCallback0(@NonNull String invokeId) {
        this.emit("callback:" + invokeId + ":remove", null);
    }

    public void injectBridgeScript(@NonNull CefFrame frame) {
        // Inject the bridge script.
        this.frame = frame;
        this.injectAndInit();
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
