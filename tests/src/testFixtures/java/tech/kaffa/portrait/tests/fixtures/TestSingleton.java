package tech.kaffa.portrait.tests.fixtures;

import tech.kaffa.portrait.Reflective;

@Reflective
public final class TestSingleton {

    public static final TestSingleton INSTANCE = new TestSingleton();

    private TestSingleton() {
    }

    public String getSingletonValue() {
        return "singleton";
    }
}

