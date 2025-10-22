package tech.kaffa.portrait.tests.fixtures;

import tech.kaffa.portrait.Reflective;

@Reflective
public class ServiceClass {

    private int state = 0;

    public int increment() {
        state++;
        return state;
    }

    public void reset() {
        state = 0;
    }

    public int getState() {
        return state;
    }

    public String processString(String input) {
        return input.toUpperCase();
    }

    public int processVarargs(int... numbers) {
        int sum = 0;
        for (int number : numbers) {
            sum += number;
        }
        return sum;
    }
}

