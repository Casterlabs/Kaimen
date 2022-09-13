package co.casterlabs.kaimen.webview.bridge;

import java.lang.ref.WeakReference;

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
 * callback.invoke(1, 2, "3"); // Mix types, do whatever you want, yada yada.
 * </pre>
 * 
 * @implNote This is actually just a JsonObject in disguise.
 */
@JsonClass(exposeAll = true)
public class JavascriptCallback {
    private String invokeId;
    WeakReference<WebviewBridge> $bridge;

    public void invoke(@NonNull Object... args) {
        JsonArray arguments = (JsonArray) Rson.DEFAULT.toJson(args);

        if ($bridge.get() != null) {
            $bridge.get().invokeCallback(this.invokeId, arguments);
        }
    }

    @Override
    public int hashCode() {
        return this.invokeId.hashCode();
    }

    @Override
    protected void finalize() throws Throwable {
        if ($bridge.get() != null) {
            $bridge.get().removeCallback(this.invokeId);
        }
    }

}
