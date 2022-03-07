package co.casterlabs.kaimen.webview.bridge;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import co.casterlabs.rakurai.json.Rson;
import co.casterlabs.rakurai.json.element.JsonArray;
import co.casterlabs.rakurai.json.element.JsonElement;
import co.casterlabs.rakurai.json.element.JsonString;
import co.casterlabs.rakurai.json.serialization.JsonParseException;
import lombok.Getter;
import lombok.NonNull;
import xyz.e3ndr.reflectionlib.helpers.AccessHelper;

public abstract class JavascriptObject {
    private @Getter String id = UUID.randomUUID().toString();

    private Map<String, FieldMapping> properties = new HashMap<>();
    private Map<String, MethodMapping> functions = new HashMap<>();

    public JavascriptObject() {
        for (Field field : this.getClass().getDeclaredFields()) {
            if (field.isAnnotationPresent(JavascriptValue.class)) {
                JavascriptValue annotation = field.getAnnotation(JavascriptValue.class);
                String name = annotation.value().isEmpty() ? field.getName() : annotation.value();

                FieldMapping mapping = new FieldMapping(this);

                mapping.value = field;
                AccessHelper.makeAccessible(field);

                this.properties.put(name, mapping);
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
                    mapping = new FieldMapping(this);
                    this.properties.put(name, mapping);
                }

                mapping.getter = method;
                AccessHelper.makeAccessible(method);
            } else if (method.isAnnotationPresent(JavascriptSetter.class)) {
                JavascriptSetter annotation = method.getAnnotation(JavascriptSetter.class);
                String name = annotation.value().isEmpty() ? method.getName() : annotation.value();

                FieldMapping mapping = this.properties.get(name);

                if (mapping == null) {
                    mapping = new FieldMapping(this);
                    this.properties.put(name, mapping);
                }

                mapping.setter = method;
                AccessHelper.makeAccessible(method);
            }
        }

    }

    void init(String name, WebviewBridge bridge) {
        bridge.eval(
            String.format("window.Bridge.internal_define(%s,%s);", new JsonString(name), new JsonString(this.id))
        );

        for (String functionName : this.functions.keySet()) {
            bridge.eval(
                String.format("window[%s].__deffun(%s);", new JsonString(name), new JsonString(functionName))
            );
        }
    }

    public @Nullable JsonElement get(@NonNull String property) throws Throwable {
        try {
            FieldMapping mapping = this.properties.get(property);
            assert mapping != null : "Could not find property: " + property;

            return mapping.get();
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }

    public void set(@NonNull String property, JsonElement value) throws Throwable {
        try {
            FieldMapping mapping = this.properties.get(property);
            assert mapping != null : "Could not find property: " + property;

            mapping.set(value);
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }

    public @Nullable JsonElement invoke(@NonNull String function, @NonNull JsonArray arguments) throws Throwable {
        try {
            MethodMapping mapping = this.functions.get(function);
            assert mapping != null : "Could not find function: " + function;

            return mapping.invoke(arguments);
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

        public @Nullable JsonElement invoke(@NonNull JsonArray arguments) throws Exception {
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

            Object result = this.method.invoke($i, args);

            return Rson.DEFAULT.toJson(result);
        }

    }

    private static class FieldMapping {
        private @NonNull Object $i;

        private Method getter;
        private Method setter;
        private Field value;

        public FieldMapping(Object i) {
            this.$i = i;
        }

        public void set(@NonNull JsonElement v) throws Exception {
            if (this.setter != null) {
                Object o = null;

                if (!v.isJsonNull()) {
                    Class<?> type = this.setter.getParameterTypes()[0];
                    o = Rson.DEFAULT.fromJson(v, type);
                }

                this.setter.invoke($i, o);
            } else {
                Object o = null;

                if (!v.isJsonNull()) {
                    Class<?> type = this.value.getType();
                    o = Rson.DEFAULT.fromJson(v, type);
                }

                this.value.set($i, o);
            }
        }

        public @Nullable JsonElement get() throws Exception {
            Object result;

            if (this.getter != null) {
                result = this.getter.invoke($i);
            } else {
                result = this.value.get($i);
            }

            return Rson.DEFAULT.toJson(result);
        }

    }

}
