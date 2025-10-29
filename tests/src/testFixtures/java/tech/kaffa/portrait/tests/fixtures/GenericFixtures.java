package tech.kaffa.portrait.tests.fixtures;

import tech.kaffa.portrait.Includes;
import tech.kaffa.portrait.Reflective;

import java.util.ArrayList;
import java.util.List;

@Reflective(including = {Includes.PUBLIC_API_SUPERTYPES})
public class GenericFixtures<T> {

    public List<String> stringList() {
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
