package co.casterlabs.kaimen.app;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import co.casterlabs.kaimen.app.IpcPacketInvocationResult.ResultData;
import co.casterlabs.rakurai.json.Rson;
import co.casterlabs.rakurai.json.annotating.JsonField;
import co.casterlabs.rakurai.json.element.JsonArray;
import co.casterlabs.rakurai.json.element.JsonObject;
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

        Class<?> interfaceClazz = this.getInterfaceClass();

        for (Method method : this.getClass().getDeclaredMethods()) {
            boolean implementsMethod = hasMethod(interfaceClazz, method.getName(), method.getParameterTypes());

            if (implementsMethod) {
                AccessHelper.makeAccessible(method);

                this.methods.put(
                    method.getName(),
                    new MethodMapping(this, method)
                );
            }
        }
    }

    public final Class<?> getInterfaceClass() {
        Class<?> clazz = this.getInterfaceClass0();

        assert clazz.isAssignableFrom(this.getClass()) : "Your ipc object class should extend your implementing class.";
        assert clazz.isInterface() : "Your implementing class should be an interface.";

        return clazz;
    }

    protected abstract Class<?> getInterfaceClass0();

    @Override
    protected void finalize() throws Throwable {
        objects.remove(this.id);
    }

    public static IpcObject getObject(@NonNull String id) {
        WeakReference<IpcObject> $ref = objects.get(id);
        assert $ref != null : "That object doesn't exist.";
        return $ref.get();
    }

    public @Nullable ResultData invoke(@NonNull String method, @NonNull JsonArray arguments) throws Throwable {
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

        public @Nullable ResultData invoke(@NonNull JsonArray arguments) throws Exception {
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

            if (result instanceof IpcObject) {
                IpcObject obj = ((IpcObject) result);

                JsonObject resultElement = new JsonObject()
                    .put("objectId", obj.id)
                    .put("objectInterface", obj.getInterfaceClass().getCanonicalName());

                return new ResultData(
                    false,
                    resultElement
                );
            } else {
                return new ResultData(
                    true,
                    Rson.DEFAULT.toJson(result)
                );
            }
        }
    }

    private static boolean hasMethod(Class<?> clazz, String name, Class<?>[] parameterTypes) {
        try {
            clazz.getMethod(name, parameterTypes);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

}
