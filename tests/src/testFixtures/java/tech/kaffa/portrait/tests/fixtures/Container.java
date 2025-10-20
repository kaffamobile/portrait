package tech.kaffa.portrait.tests.fixtures;

import java.util.function.Function;
import tech.kaffa.portrait.Reflective;

@Reflective
public class Container<T> {

    private final T content;

    public Container(T content) {
        this.content = content;
    }

    public T get() {
        return content;
    }

    public Container<T> transform(Function<T, T> transformer) {
        return new Container<>(transformer.apply(content));
    }
}

