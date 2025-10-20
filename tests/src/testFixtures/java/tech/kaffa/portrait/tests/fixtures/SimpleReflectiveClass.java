package tech.kaffa.portrait.tests.fixtures;

import tech.kaffa.portrait.Includes;
import tech.kaffa.portrait.Reflective;

@Reflective(including = {Includes.PUBLIC_API})
public class SimpleReflectiveClass {

    public static final int DEFAULT_VALUE = 42;

    private final String name;
    private final int value;

    public SimpleReflectiveClass(String name, int value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public int getValue() {
        return value;
    }

    public String greet() {
        return "Hello, " + name + "!";
    }

    public int calculate(int multiplier) {
        return value * multiplier;
    }

    public static SimpleReflectiveClass create(String name) {
        return new SimpleReflectiveClass(name, DEFAULT_VALUE);
    }
}

