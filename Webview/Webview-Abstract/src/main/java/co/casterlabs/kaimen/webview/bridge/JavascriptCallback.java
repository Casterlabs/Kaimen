package co.casterlabs.kaimen.webview.bridge;

import co.casterlabs.rakurai.json.Rson;
import co.casterlabs.rakurai.json.annotating.JsonClass;
import co.casterlabs.rakurai.json.element.JsonArray;
import lombok.NonNull;

/**
 * This class allows you to pass in JavaScript functions and invoke them in
 * Java.
 * 
 * <br />
 * <br />
 * 
 * JS:
 * 
 * <pre>
 *  window.myObject.on("something", (...args) => Console.log("test!", args));
 * </pre>
 * 
 * Java:
 * 
 * <pre>
 * &#64;JavascriptFunction
 * public void on(String type, JavascriptCallback callback) {
 *     // Register it somewhere....
 * }
 * 
 * // ...
 * 
 * callback.invoke(bridgeCtx, 1, 2, "3"); // Mix types, do whatever you want, yada yada.
 * </pre>
 * 
 * @implNote This is actually just a JsonObject in disguise.
 */
@JsonClass(exposeAll = true)
public class JavascriptCallback {
    private String invokeId;

    public void invoke(@NonNull WebviewBridge ctx, @NonNull Object... args) {
        JsonArray arguments = (JsonArray) Rson.DEFAULT.toJson(args);

        ctx.invokeCallback(this.invokeId, arguments);
    }

    @Override
    public int hashCode() {
        return this.invokeId.hashCode();
    }

}
