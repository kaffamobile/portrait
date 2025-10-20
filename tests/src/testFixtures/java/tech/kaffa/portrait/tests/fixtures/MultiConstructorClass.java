package tech.kaffa.portrait.tests.fixtures;

import tech.kaffa.portrait.Includes;
import tech.kaffa.portrait.Reflective;

@Reflective(including = {Includes.PUBLIC_API})
public class MultiConstructorClass {

    private final String name;
    private final int value;
    private final String optional;

    public MultiConstructorClass(String name) {
        this(name, 0, null);
    }

    public MultiConstructorClass(String name, int value) {
        this(name, value, null);
    }

    public MultiConstructorClass(String name, int value, String optional) {
        this.name = name;
        this.value = value;
        this.optional = optional;
    }

    public String getName() {
        return name;
    }

    public int getValue() {
        return value;
    }

    public String getOptional() {
        return optional;
    }
}

