package tech.kaffa.portrait.tests.fixtures;

@TestAnnotation(value = "class-level", number = 100, flag = true)
public class AnnotatedTestClass {

    @TestAnnotation("field-level")
    public String annotatedField = "test";

    @TestAnnotation(value = "method-level", number = 200)
    public String annotatedMethod() {
        return "annotated";
    }
}

