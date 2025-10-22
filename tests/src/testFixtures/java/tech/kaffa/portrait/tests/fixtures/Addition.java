package tech.kaffa.portrait.tests.fixtures;

import java.util.Objects;
import tech.kaffa.portrait.Reflective;

@Reflective
public class Addition extends Operation {

    private final int a;
    private final int b;

    public Addition(int a, int b) {
        this.a = a;
        this.b = b;
    }

    public int getA() {
        return a;
    }

    public int getB() {
        return b;
    }

    @Override
    public int execute() {
        return a + b;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Addition)) {
            return false;
        }
        Addition addition = (Addition) o;
        return a == addition.a && b == addition.b;
    }

    @Override
    public int hashCode() {
        return Objects.hash(a, b);
    }
}

