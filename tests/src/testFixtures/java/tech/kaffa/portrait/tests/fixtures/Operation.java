package tech.kaffa.portrait.tests.fixtures;

import tech.kaffa.portrait.Includes;
import tech.kaffa.portrait.Reflective;

@Reflective(including = {Includes.ALL_SUBTYPES})
public abstract class Operation {

    public abstract int execute();
}

