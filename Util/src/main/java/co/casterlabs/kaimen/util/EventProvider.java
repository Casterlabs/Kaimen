package co.casterlabs.kaimen.util;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;

import lombok.NonNull;
import xyz.e3ndr.fastloggingframework.logging.FastLogger;
import xyz.e3ndr.fastloggingframework.logging.LogLevel;

public class EventProvider<T> {
    private Map<Integer, Consumer<T>> listeners = new HashMap<>();

    /* on */

    public synchronized int on(@NonNull Consumer<T> listener) {
        int id = listener.hashCode();

        assert !this.listeners.containsKey(id) : "That listener is already registered elsewhere.";

        this.listeners.put(id, listener);

        return id;
    }

    public synchronized int on(@NonNull Runnable listener) {
        return this.on((aVoid) -> listener.run());
    }

    /* off */

    public synchronized void off(@NonNull Consumer<T> listener) {
        this.off(listener.hashCode());
    }

    public synchronized void off(@NonNull Runnable listener) {
        this.off(listener.hashCode());
    }

    public synchronized void off(int id) {
        this.listeners.remove(id);
    }

    /* Firing */

    public synchronized void fireEvent(@Nullable T data) {
        for (Consumer<T> listener : this.listeners.values()) {
            try {
                listener.accept(data);
            } catch (Throwable t) {
                FastLogger.logStatic(LogLevel.SEVERE, "An error occured whilst processing an event.");
                FastLogger.logException(t);
            }
        }
    }

}
