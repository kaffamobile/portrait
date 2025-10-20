package tech.kaffa.portrait.tests.fixtures;

import tech.kaffa.portrait.Reflective;

@TestAnnotation(value = "class-level", number = 100, flag = true)
@Reflective
public class AnnotatedTestClass {

    @TestAnnotation("field-level")
    public String annotatedField = "test";

    @TestAnnotation(value = "method-level", number = 200)
    public String annotatedMethod() {
        return "annotated";
    }
}

