package tech.kaffa.portrait.tests.fixtures;

import tech.kaffa.portrait.Reflective;

@Reflective
public class TestClass implements TestInterface {

    private String internalValue = "initial";

    public TestClass() {
        this("default");
    }

    public TestClass(String value) {
        this.internalValue = value;
    }

    @Override
    public String doSomething() {
        return "did something with " + internalValue;
    }

    @Override
    public int processValue(int value) {
        return value * 2;
    }

    @Override
    public String getName() {
        return internalValue;
    }

    public void setInternalValue(String value) {
        this.internalValue = value;
    }

    public String getInternalValue() {
        return internalValue;
    }

    public static String staticMethod() {
        return "static result";
    }
}

