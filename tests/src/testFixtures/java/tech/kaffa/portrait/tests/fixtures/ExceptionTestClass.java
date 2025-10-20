package tech.kaffa.portrait.tests.fixtures;

import tech.kaffa.portrait.Reflective;

@Reflective
public class ExceptionTestClass {

    public String throwsException() {
        throw new RuntimeException("Test exception");
    }

    public String throwsChecked() throws Exception {
        throw new Exception("Checked exception");
    }
}

