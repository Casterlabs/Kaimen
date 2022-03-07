package co.casterlabs.kaimen.util.functional;

import lombok.AllArgsConstructor;

@AllArgsConstructor
@SuppressWarnings("unchecked")
public class Either<A, B> {
    private Object value;

    public A a() {
        return (A) this.value;
    }

    public B b() {
        return (B) this.value;
    }

}
