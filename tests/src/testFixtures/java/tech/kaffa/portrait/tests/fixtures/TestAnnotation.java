package tech.kaffa.portrait.tests.fixtures;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import tech.kaffa.portrait.Includes;
import tech.kaffa.portrait.ProxyTarget;
import tech.kaffa.portrait.Reflective;

@ProxyTarget
@Reflective(including = {Includes.ALL_SUPERTYPES})
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface TestAnnotation {

    String value() default "default";

    int number() default 42;

    boolean flag() default false;
}

