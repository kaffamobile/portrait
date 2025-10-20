package tech.kaffa.portrait.tests.fixtures;

import tech.kaffa.portrait.Reflective;

@Reflective
public final class SingletonService {

    public static final SingletonService INSTANCE = new SingletonService();

    private int counter = 0;

    private SingletonService() {
    }

    public int incrementCounter() {
        counter++;
        return counter;
    }

    public int getCounter() {
        return counter;
    }
}

