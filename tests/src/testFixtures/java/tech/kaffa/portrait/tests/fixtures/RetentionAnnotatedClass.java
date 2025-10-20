package tech.kaffa.portrait.tests.fixtures;

import tech.kaffa.portrait.Reflective;

@Reflective
@ClassRetentionAnnotation("class-level")
@TestAnnotation(value = "runtime-class", number = 7)
public class RetentionAnnotatedClass {

    @ClassRetentionAnnotation("field-level")
    @TestAnnotation("runtime-field")
    public String annotatedField = "value";

    @ClassRetentionAnnotation("constructor-level")
    public RetentionAnnotatedClass() {
    }

    @ClassRetentionAnnotation("method-level")
    @TestAnnotation("runtime-method")
    public String annotatedMethod() {
        return "method";
    }

    @SourceRetentionAnnotation("source-only")
    public void sourceAnnotatedMethod() {
    }
}
