package tech.kaffa.portrait.tests.fixtures;

import tech.kaffa.portrait.Reflective;

@Reflective
public class NullableTestClass {

    private final String required;
    private final String optional;

    public NullableTestClass(String required, String optional) {
        this.required = required;
        this.optional = optional;
    }

    public String getRequired() {
        return required;
    }

    public String getOptional() {
        return optional;
    }

    public String getOptionalOrDefault(String defaultValue) {
        return optional != null ? optional : defaultValue;
    }
}

