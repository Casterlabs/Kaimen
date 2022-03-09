package co.casterlabs.kaimen.util.reflection;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;

import co.casterlabs.kaimen.util.threading.AsyncTask;
import lombok.NonNull;
import lombok.SneakyThrows;
import xyz.e3ndr.reflectionlib.helpers.AccessHelper;

/**
 * This basically works by checking the hashCode of the value in a field and
 * comparing it against a known value.
 */
public class FieldMutationListener {
    private static @Reflective int POLL_INTERVAL = 75;
    private static final int NULL_V = 0;

    private Field field;
    private WeakReference<Object> $inst;
    private AsyncTask task;
    private int lastHash;

    private @Nullable Consumer<@Nullable Object> onMutate;

    public FieldMutationListener(@NonNull Field field, @Nullable Object instance) {
        this(field, instance != null ? new WeakReference<>(instance) : null);
    }

    public FieldMutationListener(@NonNull Field field, @Nullable WeakReference<Object> instance) {
        AccessHelper.makeAccessible(field);

        this.field = field;
        this.$inst = instance;
        this.lastHash = getHashCodeForField(this.field, $inst != null ? $inst.get() : null);

        this.task = new AsyncTask(this::asyncChecker);
    }

    @SuppressWarnings("unchecked")
    public <T> void onMutate(@Nullable final Consumer<@Nullable T> consumer) {
        if (consumer == null) {
            this.onMutate = null;
        } else {
            this.onMutate = (@Nullable Object v) -> {
                consumer.accept((T) v);
            };
        }
    }

    @SneakyThrows
    private void asyncChecker() {
        while (true) {
            Object instance = null;

            if ($inst != null) {
                instance = $inst.get();

                if (instance == null) {
                    return;
                }
            }

            int currentHash = getHashCodeForField(this.field, instance);

            if (this.lastHash != currentHash) {
                this.lastHash = currentHash;

                if (this.onMutate != null) {
                    this.onMutate.accept(
                        this.field.get(instance)
                    );
                }
            }

            Thread.yield();
            Thread.sleep(POLL_INTERVAL);
        }
    }

    public void stopWatching() {
        this.task.cancel();
    }

    @SneakyThrows
    private static int getHashCodeForField(Field field, Object instance) {
        Object v = field.get(instance);

        if (v == null) {
            return NULL_V;
        } else {
            return v.hashCode();
        }
    }

}
