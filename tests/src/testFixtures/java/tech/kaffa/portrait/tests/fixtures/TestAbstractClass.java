package tech.kaffa.portrait.tests.fixtures;

import tech.kaffa.portrait.Reflective;

@Reflective
public abstract class TestAbstractClass {

    public abstract String abstractMethod();

    public String concreteMethod() {
        return "concrete";
    }

    public String finalMethod() {
        return "final";
    }
}

