package tech.kaffa.portrait.tests.fixtures;

import tech.kaffa.portrait.ProxyTarget;

@ProxyTarget
public interface Calculator {

    int add(int a, int b);

    int multiply(int a, int b);
}

