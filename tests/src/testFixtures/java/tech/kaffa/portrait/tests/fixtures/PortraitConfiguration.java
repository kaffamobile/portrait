package tech.kaffa.portrait.tests.fixtures;

import tech.kaffa.portrait.Reflective;

@Reflective.Include(classes = {
        Object.class, String.class, Integer.class
})
public class PortraitConfiguration {
}
