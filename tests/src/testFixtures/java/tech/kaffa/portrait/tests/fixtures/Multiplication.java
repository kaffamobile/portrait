package tech.kaffa.portrait.tests.fixtures;

import java.util.Objects;
import tech.kaffa.portrait.Reflective;

@Reflective
public class Multiplication extends Operation {

    private final int a;
    private final int b;

    public Multiplication(int a, int b) {
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
        return a * b;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Multiplication)) {
            return false;
        }
        Multiplication that = (Multiplication) o;
        return a == that.a && b == that.b;
    }

    @Override
    public int hashCode() {
        return Objects.hash(a, b);
    }
}

