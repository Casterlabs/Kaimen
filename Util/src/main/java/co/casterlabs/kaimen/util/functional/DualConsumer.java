package co.casterlabs.kaimen.util.functional;

public interface DualConsumer<T, D> {

    public void accept(T type, D data);

}
