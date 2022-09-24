package co.casterlabs.kaimen.webview.bridge;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import org.jetbrains.annotations.Nullable;

import co.casterlabs.kaimen.webview.Webview;
import co.casterlabs.kaimen.webview.WebviewFileUtil;
import co.casterlabs.rakurai.json.element.JsonArray;
import co.casterlabs.rakurai.json.element.JsonElement;
import co.casterlabs.rakurai.json.element.JsonObject;
import lombok.NonNull;
import lombok.Setter;

public abstract class WebviewBridge {
    private static String bridgeScript = "";

    private static List<WeakReference<WebviewBridge>> bridges = new ArrayList<>();

    private WeakReference<WebviewBridge> $ref = new WeakReference<>(this);

    private List<WeakReference<WebviewBridge>> downstreamBridges = new LinkedList<>();
    private List<WeakReference<WebviewBridge>> attachedBridges = new LinkedList<>();

    Map<String, JavascriptObject> objects = new HashMap<>();

    protected @Setter BiConsumer<String, JsonObject> onEvent;

    static {
        try {
            bridgeScript = WebviewFileUtil.loadResourceFromBuildProject("WebviewBridge.js", "Webview-Abstract");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public WebviewBridge() {
        bridges.add(this.$ref);
    }

    @Override
    protected void finalize() {
        bridges.remove(this.$ref);
    }

    /**
     * This method allows you to merge two bridges without needing to maintain two
     * objects. Calling this will make this bridge inherit all events and objects
     * from the parent bridge.
     */
    public void join(@NonNull WebviewBridge parent) {
        parent.downstreamBridges.add(this.$ref);
        this.attachedBridges.add(parent.$ref);

        // Pointer copy.
        this.objects = parent.objects;

        // This allows the parent's onEvent to change and for this object to still
        // respect that change. Note that this behavior can still be undone by calling
        // #setOnEvent() on this object.
        this.onEvent = (str, obj) -> {
            if (parent.onEvent != null) {
                parent.onEvent.accept(str, obj);
            }
        };
    }

    public void defineObject(@NonNull String name, @NonNull JavascriptObject obj) {
        obj.init(name, this);
    }

    public void emit(@NonNull String type, @NonNull JsonElement data) {
        this.emit0(type, data);

        for (WeakReference<WebviewBridge> wb : this.downstreamBridges) {
            wb.get().emit0(type, data);
        }
    }

    /**
     * @implNote This differs from {@link Webview#executeJavaScript(String)} in that
     *           it also calls eval on all the downstream bridges (ones that
     *           join()'ed with this bridge)
     */
    public void eval(@NonNull String script) {
        this.eval0(script);

        for (WeakReference<WebviewBridge> wb : this.downstreamBridges) {
            wb.get().eval0(script);
        }
    }

    protected String getInjectScript() {
        return String.format(
            "try { %s } catch (e) { alert(e); } finally { console.log('[Kaimen]', 'Bridge inject completed.'); }",
            bridgeScript.replace("\"replace with native comms code\";", this.getNativeBridgeScript())
        );
    }

    protected void injectAndInit() {
        this.eval0(this.getInjectScript());
        this.init();
    }

    protected void init() {
        for (Map.Entry<String, JavascriptObject> entry : new ArrayList<>(this.objects.entrySet())) {
            if (!entry.getKey().contains(".")) {
                entry.getValue().init(entry.getKey(), this);
            }
        }

        for (WeakReference<WebviewBridge> wb : this.attachedBridges) {
            wb.get().downstreamBridges.remove(this.$ref);
        }

        this.eval0("console.log('[Kaimen]', 'Bridge init completed.');");
    }

    protected @Nullable JsonElement processGet(String id, String property) throws Throwable {
        for (JavascriptObject obj : this.objects.values()) {
            if (obj.getId().equals(id)) {
                return obj.get(property, this);
            }
        }

        return null;
    }

    protected @Nullable JsonElement processInvoke(String id, String function, JsonArray args) throws Throwable {
        for (JavascriptObject obj : this.objects.values()) {
            if (obj.getId().equals(id)) {
                return obj.invoke(function, args, this);
            }
        }

        return null;
    }

    protected void processSet(String id, String property, JsonElement value) throws Throwable {
        for (JavascriptObject obj : this.objects.values()) {
            if (obj.getId().equals(id)) {
                obj.set(property, value, this);
                break;
            }
        }
    }

    void invokeCallback(@NonNull String invokeId, @NonNull JsonArray arguments) {
        this.invokeCallback0(invokeId, arguments);

        for (WeakReference<WebviewBridge> wb : this.downstreamBridges) {
            wb.get().invokeCallback0(invokeId, arguments);
        }
    }

    void removeCallback(@NonNull String invokeId) {
        this.removeCallback0(invokeId);

        for (WeakReference<WebviewBridge> wb : this.downstreamBridges) {
            wb.get().removeCallback0(invokeId);
        }
    }

    /* Impl */

    protected abstract void emit0(@NonNull String type, @NonNull JsonElement data);

    protected abstract void eval0(@NonNull String script);

    protected abstract void invokeCallback0(@NonNull String invokeId, @NonNull JsonArray arguments);

    protected abstract void removeCallback0(@NonNull String invokeId);

    protected abstract String getNativeBridgeScript();

    /* Statics */

    public static void emitAll(@NonNull String type, @NonNull JsonElement data) {
        bridges.forEach((b) -> b.get().emit0(type, data));
    }

    public static void evalAll(@NonNull String script) {
        bridges.forEach((b) -> b.get().eval0(script));
    }

}
