package co.casterlabs.kaimen.webview.bridge;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import co.casterlabs.kaimen.util.functional.DualConsumer;
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

    protected @Setter DualConsumer<String, JsonObject> onEvent;

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

    @Deprecated
    public void mergeWith(WebviewBridge parent) {
        parent.downstreamBridges.add(this.$ref);
        this.attachedBridges.add(parent.$ref);
        this.objects = parent.objects; // Pointer copy.
        this.onEvent = parent.onEvent;
    }

    @Override
    protected void finalize() {
        bridges.remove(this.$ref);
    }

    protected void init() {
        String script = bridgeScript.replace("\"replace with native comms code\";", this.getNativeBridgeScript());
        this.eval0(script);

        for (Map.Entry<String, JavascriptObject> entry : new ArrayList<>(this.objects.entrySet())) {
            if (!entry.getKey().contains(".")) {
                entry.getValue().init(entry.getKey(), this);
            }
        }

        for (WeakReference<WebviewBridge> wb : this.attachedBridges) {
            wb.get().downstreamBridges.remove(this.$ref);
        }
    }

    public void defineObject(@NonNull String name, @NonNull JavascriptObject obj) {
        obj.init(name, this);
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
            }
        }
    }

    public void emit(@NonNull String type, @NonNull JsonElement data) {
        this.emit0(type, data);

        for (WeakReference<WebviewBridge> wb : this.downstreamBridges) {
            wb.get().emit0(type, data);
        }
    }

    public synchronized void eval(@NonNull String script) {
        this.eval0(script);

        for (WeakReference<WebviewBridge> wb : this.downstreamBridges) {
            wb.get().eval0(script);
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
