package co.casterlabs.kaimen.util;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;

import lombok.NonNull;
import xyz.e3ndr.fastloggingframework.logging.FastLogger;
import xyz.e3ndr.fastloggingframework.logging.LogLevel;

public class MultiEventProvider<T> {
    private Map<String, Map<Integer, Consumer<T>>> listenerSections = new HashMap<>();
    private Set<Integer> allListeners = new HashSet<>();

    /* on */

    public synchronized int on(@NonNull String type, @NonNull Consumer<T> listener) {
        int id = listener.hashCode();

        assert !this.allListeners.contains(id) : "That listener is already registered elsewhere.";

        Map<Integer, Consumer<T>> listenerSection = this.listenerSections.get(type);

        if (listenerSection == null) {
            listenerSection = new HashMap<>();
            this.listenerSections.put(type, listenerSection);
        }

        listenerSection.put(id, listener);
        this.allListeners.add(id);

        return id;
    }

    public synchronized int on(@NonNull String type, @NonNull Runnable listener) {
        return this.on(type, (aVoid) -> listener.run());
    }

    /* off */

    public synchronized void off(@NonNull Consumer<T> listener) {
        this.off(listener.hashCode());
    }

    public synchronized void off(@NonNull Runnable listener) {
        this.off(listener.hashCode());
    }

    public synchronized void off(int id) {
        this.allListeners.remove(id);

        for (Map<Integer, Consumer<T>> listenerSection : this.listenerSections.values()) {
            listenerSection.remove(id);
        }
    }

    /* Firing */

    public synchronized void fireEvent(@NonNull String type, @Nullable T data) {
        Map<Integer, Consumer<T>> listenerSection = this.listenerSections.get(type);

        if (listenerSection != null) {
            for (Consumer<T> listener : listenerSection.values()) {
                try {
                    listener.accept(data);
                } catch (Throwable t) {
                    FastLogger.logStatic(LogLevel.SEVERE, "An error occured whilst processing an event of type %s.", type);
                    FastLogger.logException(t);
                }
            }
        }
    }

}
