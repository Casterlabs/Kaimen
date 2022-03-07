package co.casterlabs.kaimen.webview.bridge;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import co.casterlabs.kaimen.util.functional.DualConsumer;
import co.casterlabs.rakurai.json.element.JsonArray;
import co.casterlabs.rakurai.json.element.JsonElement;
import co.casterlabs.rakurai.json.element.JsonObject;
import lombok.NonNull;
import lombok.Setter;

public abstract class WebviewBridge {
    private static Map<String, BridgeValue<?>> globalQueryData = new HashMap<>();
    private static List<WeakReference<WebviewBridge>> bridges = new ArrayList<>();

    private WeakReference<WebviewBridge> $ref = new WeakReference<>(this);

    private Map<String, BridgeValue<?>> personalQueryData = new HashMap<>();
    private Map<String, JavascriptObject> objects = new HashMap<>();

    protected @Setter DualConsumer<String, JsonObject> onEvent;

    public WebviewBridge() {
        bridges.add(this.$ref);
    }

    public void attachValue(@NonNull BridgeValue<?> bv) {
        this.personalQueryData.put(bv.getKey(), bv);
        bv.attachedBridges.add(this.$ref);
    }

    protected Map<String, BridgeValue<?>> getQueryData() {
        Map<String, BridgeValue<?>> combined = new HashMap<>();

        combined.putAll(this.personalQueryData);
        combined.putAll(globalQueryData);

        return combined;
    }

    @Override
    protected void finalize() {
        bridges.remove(this.$ref);

        for (BridgeValue<?> bv : this.personalQueryData.values()) {
            bv.attachedBridges.remove(this.$ref);
        }
    }

    protected void init() {
        for (Map.Entry<String, JavascriptObject> entry : this.objects.entrySet()) {
            entry.getValue().init(entry.getKey(), this);
        }
    }

    public void defineObject(@NonNull String name, @NonNull JavascriptObject obj) {
        this.objects.put(name, obj);
        obj.init(name, this);
    }

    protected @Nullable JsonElement processGet(String id, String property) throws Throwable {
        for (JavascriptObject obj : this.objects.values()) {
            if (obj.getId().equals(id)) {
                return obj.get(property);
            }
        }

        return null;
    }

    protected @Nullable JsonElement processInvoke(String id, String function, JsonArray args) throws Throwable {
        for (JavascriptObject obj : this.objects.values()) {
            if (obj.getId().equals(id)) {
                return obj.invoke(function, args);
            }
        }

        return null;
    }

    protected void processSet(String id, String property, JsonElement value) throws Throwable {
        for (JavascriptObject obj : this.objects.values()) {
            if (obj.getId().equals(id)) {
                obj.set(property, value);
            }
        }
    }

    /* Impl */

    public abstract void emit(@NonNull String type, @NonNull JsonElement data);

    public abstract void eval(@NonNull String script);

    public abstract void invokeCallback(@NonNull String invokeId, @NonNull JsonArray arguments);

    /* Statics */

    protected static void attachGlobalBridge(@NonNull BridgeValue<?> bv) {
        globalQueryData.put(bv.getKey(), bv);
    }

    public static void emitAll(@NonNull String type, @NonNull JsonElement data) {
        bridges.forEach((b) -> b.get().emit(type, data));
    }

    public static void evalAll(@NonNull String script) {
        bridges.forEach((b) -> b.get().eval(script));
    }

}
