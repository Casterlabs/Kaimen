package co.casterlabs.kaimen.app;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import co.casterlabs.rakurai.json.Rson;
import co.casterlabs.rakurai.json.annotating.JsonField;
import co.casterlabs.rakurai.json.element.JsonArray;
import co.casterlabs.rakurai.json.element.JsonElement;
import co.casterlabs.rakurai.json.serialization.JsonParseException;
import lombok.Getter;
import lombok.NonNull;
import lombok.SneakyThrows;
import xyz.e3ndr.reflectionlib.helpers.AccessHelper;

/**
 * This class is implemented on the Client side.
 */
public abstract class IpcObject {
    private static Map<String, WeakReference<IpcObject>> objects = new HashMap<>();

    private @JsonField @Getter String id = UUID.randomUUID().toString();

    private Map<String, MethodMapping> methods = new HashMap<>();
    private Map<String, Field> subObjects = new HashMap<>();

    @SneakyThrows
    public IpcObject() {
        objects.put(this.id, new WeakReference<>(this));

        for (Field field : this.getClass().getDeclaredFields()) {
            if (IpcObject.class.isAssignableFrom(field.getType())) {
                AccessHelper.makeAccessible(field);

                this.subObjects.put(
                    field.getName(),
                    field
                );
            }
        }

        for (Method method : this.getClass().getDeclaredMethods()) {
            if (method.isAnnotationPresent(IpcMethod.class)) {
                AccessHelper.makeAccessible(method);

                this.methods.put(
                    method.getName(),
                    new MethodMapping(this, method)
                );
            }
        }
    }

    @Override
    protected void finalize() throws Throwable {
        objects.remove(this.id);
    }

    public static IpcObject getObject(@NonNull String id) {
        WeakReference<IpcObject> $ref = objects.get(id);
        assert $ref != null : "That object doesn't exist.";
        return $ref.get();
    }

    public @Nullable JsonElement invoke(@NonNull String method, @NonNull JsonArray arguments) throws Throwable {
        try {
            MethodMapping mapping = this.methods.get(method);
            assert mapping != null : "Could not find method: " + method;

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

}
