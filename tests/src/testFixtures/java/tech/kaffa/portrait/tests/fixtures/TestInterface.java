package tech.kaffa.portrait.tests.fixtures;

import tech.kaffa.portrait.ProxyTarget;
import tech.kaffa.portrait.Reflective;

@Reflective
@ProxyTarget
public interface TestInterface {

    String doSomething();

    int processValue(int value);

    String getName();
}

