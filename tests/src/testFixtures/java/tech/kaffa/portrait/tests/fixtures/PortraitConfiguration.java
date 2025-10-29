package tech.kaffa.portrait.tests.fixtures;

import tech.kaffa.portrait.Includes;
import tech.kaffa.portrait.Reflective;

@Reflective.Include(classes = {
        Object.class, String.class, Integer.class
}, including = {Includes.ALL_SUPERTYPES})
public class PortraitConfiguration {
}
