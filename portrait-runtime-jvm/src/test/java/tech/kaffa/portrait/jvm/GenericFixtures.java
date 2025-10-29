package tech.kaffa.portrait.jvm;

import java.util.ArrayList;
import java.util.List;

public class GenericFixtures<T> {

    public List<String> strings() {
        return new ArrayList<>();
    }

    public T identity(T value) {
        return value;
    }

    public List<? extends Number> wildcardExtends() {
        return new ArrayList<>();
    }

    public List<? super Number> wildcardSuper() {
        return new ArrayList<>();
    }

    public T[] genericArray(T[] input) {
        return input;
    }
}
