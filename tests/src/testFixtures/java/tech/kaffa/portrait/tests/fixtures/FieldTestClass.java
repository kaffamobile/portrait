package tech.kaffa.portrait.tests.fixtures;

import tech.kaffa.portrait.Reflective;

@Reflective
public class FieldTestClass {

    public String publicField = "public";
    private int privateField = 42;
    private final double readOnlyField = 3.14;

    public int getPrivateField() {
        return privateField;
    }

    public void setPrivateField(int value) {
        this.privateField = value;
    }

    public double getReadOnlyField() {
        return readOnlyField;
    }
}

