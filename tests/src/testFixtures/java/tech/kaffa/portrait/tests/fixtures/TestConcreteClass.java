package tech.kaffa.portrait.tests.fixtures;

import tech.kaffa.portrait.Reflective;

@Reflective
public class TestConcreteClass extends TestAbstractClass {

    @Override
    public String abstractMethod() {
        return "implemented";
    }

    @Override
    public String concreteMethod() {
        return "overridden";
    }
}

