package co.casterlabs.kaimen.webview.bridge;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
