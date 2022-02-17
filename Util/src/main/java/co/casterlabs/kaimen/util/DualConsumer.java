package co.casterlabs.kaimen.util;

public interface DualConsumer<T, D> {

    public void accept(T type, D data);

}
