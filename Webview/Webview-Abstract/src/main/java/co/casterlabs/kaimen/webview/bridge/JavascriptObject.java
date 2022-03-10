package co.casterlabs.kaimen.webview.bridge;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import co.casterlabs.kaimen.util.reflection.FieldMutationListener;
import co.casterlabs.rakurai.json.Rson;
import co.casterlabs.rakurai.json.element.JsonArray;
import co.casterlabs.rakurai.json.element.JsonElement;
import co.casterlabs.rakurai.json.element.JsonString;
import co.casterlabs.rakurai.json.serialization.JsonParseException;
import lombok.Getter;
import lombok.NonNull;
import lombok.SneakyThrows;
import xyz.e3ndr.reflectionlib.helpers.AccessHelper;

public abstract class JavascriptObject {
    private @Getter String id = UUID.randomUUID().toString();

    private Map<String, FieldMapping> properties = new HashMap<>();
    private Map<String, MethodMapping> functions = new HashMap<>();
    private Map<String, Field> subObjects = new HashMap<>();

    private WeakReference<WebviewBridge> $bridge = new WeakReference<>(null);
    @SuppressWarnings("unused")
    private String name;

    @SneakyThrows
    public JavascriptObject() {
        for (Field field : this.getClass().getDeclaredFields()) {
            if (JavascriptObject.class.isAssignableFrom(field.getType())) {
                AccessHelper.makeAccessible(field);

                this.subObjects.put(
                    field.getName(),
                    field
                );
            } else if (field.isAnnotationPresent(JavascriptValue.class)) {
                JavascriptValue annotation = field.getAnnotation(JavascriptValue.class);
                String name = annotation.value().isEmpty() ? field.getName() : annotation.value();

                FieldMapping mapping = new FieldMapping(this, name);

                mapping.value = field;
                mapping.valueAnnotation = annotation;
                AccessHelper.makeAccessible(field);

                this.properties.put(name, mapping);

                if (annotation.watchForMutate()) {
                    new FieldMutationListener(field, this)
                        .onMutate((value) -> {
                            WebviewBridge bridge = $bridge.get();

                            if (bridge != null) {
                                bridge.eval(
                                    String.format(
                                        "window.Bridge.internal__triggermutate(`__mutate:${%s}:${%s}`,%s);",
                                        new JsonString(this.id),
                                        new JsonString(name),
                                        Rson.DEFAULT.toJson(value)
                                    )
                                );
                            }
                        });
                }
            }
        }

        for (Method method : this.getClass().getDeclaredMethods()) {
            if (method.isAnnotationPresent(JavascriptFunction.class)) {
                JavascriptFunction annotation = method.getAnnotation(JavascriptFunction.class);
                String name = annotation.value().isEmpty() ? method.getName() : annotation.value();

                this.functions.put(name, new MethodMapping(this, method));
                AccessHelper.makeAccessible(method);
            } else if (method.isAnnotationPresent(JavascriptGetter.class)) {
                JavascriptGetter annotation = method.getAnnotation(JavascriptGetter.class);
                String name = annotation.value().isEmpty() ? method.getName() : annotation.value();

                FieldMapping mapping = this.properties.get(name);

                if (mapping == null) {
                    mapping = new FieldMapping(this, name);
                    this.properties.put(name, mapping);
                }

                mapping.getter = method;
                AccessHelper.makeAccessible(method);
            } else if (method.isAnnotationPresent(JavascriptSetter.class)) {
                JavascriptSetter annotation = method.getAnnotation(JavascriptSetter.class);
                String name = annotation.value().isEmpty() ? method.getName() : annotation.value();

                FieldMapping mapping = this.properties.get(name);

                if (mapping == null) {
                    mapping = new FieldMapping(this, name);
                    this.properties.put(name, mapping);
                }

                mapping.setter = method;
                AccessHelper.makeAccessible(method);
            }
        }
    }

    void init(String name, WebviewBridge bridge) {
        this.init(name, bridge, null);
    }

    @SneakyThrows
    private void init(String name, WebviewBridge bridge, @Nullable JavascriptObject parent) {
        $bridge = new WeakReference<>(bridge);
        this.name = name;

        bridge.objects.put(name, this);

        bridge.eval(
            String.format("window.Bridge.internal__define(%s,%s);", new JsonString(name), new JsonString(this.id))
        );

        for (String functionName : this.functions.keySet()) {
            bridge.eval(
                // We directly access the property without `[]` for subobject support.
                String.format("window.%s.internal__deffun(%s,%s);", name, new JsonString(functionName), new JsonString(this.id))
            );
        }

        for (String propertyName : this.properties.keySet()) {
            bridge.eval(
                // We directly access the property without `[]` for subobject support.
                String.format("window.%s.internal__defprop(%s);", name, new JsonString(propertyName))
            );
        }

        for (Map.Entry<String, Field> entry : this.subObjects.entrySet()) {
            JavascriptObject value = (JavascriptObject) entry.getValue().get(this);

            if ((value != null) && (value != parent)) {
                value.init(name + "." + entry.getKey(), bridge, this);
            }
        }
    }

    @Nullable
    JsonElement get(@NonNull String property, @NonNull WebviewBridge bridge) throws Throwable {
        try {
            FieldMapping mapping = this.properties.get(property);
            assert mapping != null : "Could not find property: " + property;

            return mapping.get();
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }

    void set(@NonNull String property, JsonElement value, @NonNull WebviewBridge bridge) throws Throwable {
        try {
            FieldMapping mapping = this.properties.get(property);
            assert mapping != null : "Could not find property: " + property;

            mapping.set(value, bridge);
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }

    public @Nullable JsonElement invoke(@NonNull String function, @NonNull JsonArray arguments, @NonNull WebviewBridge bridge) throws Throwable {
        try {
            MethodMapping mapping = this.functions.get(function);
            assert mapping != null : "Could not find function: " + function;

            return mapping.invoke(arguments, bridge);
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }

    private static class MethodMapping {
        private Object $i;

        private Method method;

        public MethodMapping(Object i, Method method) {
            this.$i = i;
            this.method = method;
        }

        public @Nullable JsonElement invoke(@NonNull JsonArray arguments, @NonNull WebviewBridge bridge) throws Exception {
            Class<?>[] argTypes = method.getParameterTypes();
            assert argTypes.length == arguments.size() : "The invoking arguments do not match the expected length: " + argTypes.length;

            Object[] args = new Object[argTypes.length];

            for (int i = 0; i < args.length; i++) {
                try {
                    args[i] = Rson.DEFAULT.fromJson(arguments.get(i), argTypes[i]);
                } catch (JsonParseException e) {
                    throw new IllegalArgumentException("The provided argument " + arguments.get(i) + " could not be converted to " + argTypes[i].getCanonicalName());
                }
            }

            filterForCallbacks(bridge, args);

            Object result = this.method.invoke($i, args);

            return Rson.DEFAULT.toJson(result);
        }

    }

    private static class FieldMapping {
        private @NonNull Object $i;
        private @NonNull String $name;

        private Method getter;
        private Method setter;
        private Field value;
        private JavascriptValue valueAnnotation;

        public FieldMapping(Object i, String name) {
            this.$i = i;
            this.$name = name;
        }

        public void set(@NonNull JsonElement v, @NonNull WebviewBridge bridge) throws Exception {
            if (this.setter != null) {
                Object o = null;

                if (!v.isJsonNull()) {
                    Class<?> type = this.setter.getParameterTypes()[0];
                    o = Rson.DEFAULT.fromJson(v, type);
                }

                this.setter.invoke($i, o);
            } else {
                if (this.valueAnnotation.allowSet()) {
                    Object o = null;

                    if (!v.isJsonNull()) {
                        Class<?> type = this.value.getType();
                        o = Rson.DEFAULT.fromJson(v, type);
                        filterForCallbacks(bridge, o);
                    }

                    this.value.set($i, o);
                } else {
                    throw new UnsupportedOperationException("SET is not allowed for the field: " + $name);
                }
            }
        }

        public @Nullable JsonElement get() throws Exception {
            Object result;

            if (this.getter != null) {
                result = this.getter.invoke($i);
            } else {
                if (this.valueAnnotation.allowGet()) {

                    result = this.value.get($i);
                } else {
                    throw new UnsupportedOperationException("GET is not allowed for the field: " + $name);
                }
            }

            return Rson.DEFAULT.toJson(result);
        }

    }

    private static void filterForCallbacks(@NonNull WebviewBridge bridge, @Nullable Object... items) {
        if (items != null) {
            for (Object obj : items) {
                if (obj instanceof JavascriptCallback) {
                    ((JavascriptCallback) obj).$bridge = new WeakReference<>(bridge);
                }
            }
        }
    }

}
