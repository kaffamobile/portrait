package tech.kaffa.portrait.tests;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import java.lang.annotation.Annotation;
import tech.kaffa.portrait.PAnnotation;
import tech.kaffa.portrait.PClass;
import tech.kaffa.portrait.PConstructor;
import tech.kaffa.portrait.PField;
import tech.kaffa.portrait.PMethod;
import tech.kaffa.portrait.Portrait;
import tech.kaffa.portrait.tests.fixtures.ClassRetentionAnnotation;
import tech.kaffa.portrait.tests.fixtures.RetentionAnnotatedClass;
import tech.kaffa.portrait.tests.fixtures.SourceRetentionAnnotation;
import tech.kaffa.portrait.tests.fixtures.TestAnnotation;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class AnnotationRetentionTest {

    @Before
    public void clearCache() {
        Portrait.clearCache();
    }

    @Test
    public void nonSourceAnnotationsSurfaceConsistently() {
        PClass<RetentionAnnotatedClass> pClass = Portrait.of(RetentionAnnotatedClass.class);
        String providerClassName = pClass.getClass().getName();
        boolean isStaticProvider = providerClassName.contains("aot.StaticPClass");

        assertTrue(hasAnnotation(pClass.getAnnotations(), TestAnnotation.class));
        boolean classAnnotationPresent = hasAnnotation(pClass.getAnnotations(), ClassRetentionAnnotation.class);
        if (isStaticProvider) {
            assertTrue("Static providers should retain CLASS annotations", classAnnotationPresent);
        }

        assertFalse(
            "SOURCE annotations must never surface through Portrait",
            hasAnnotation(pClass.getAnnotations(), SourceRetentionAnnotation.class)
        );

        PMethod annotatedMethod = pClass.getMethod("annotatedMethod");
        assertNotNull(annotatedMethod);
        assertTrue(hasAnnotation(annotatedMethod.getAnnotations(), TestAnnotation.class));
        boolean methodHasClassAnnotation = hasAnnotation(annotatedMethod.getAnnotations(), ClassRetentionAnnotation.class);
        if (isStaticProvider) {
            assertTrue("Static providers should retain CLASS annotations on methods", methodHasClassAnnotation);
        }

        PMethod sourceAnnotatedMethod = pClass.getMethod("sourceAnnotatedMethod");
        assertNotNull(sourceAnnotatedMethod);
        assertFalse(hasAnnotation(sourceAnnotatedMethod.getAnnotations(), SourceRetentionAnnotation.class));

        PField annotatedField = pClass.getField("annotatedField");
        assertNotNull(annotatedField);
        assertTrue(hasAnnotation(annotatedField.getAnnotations(), TestAnnotation.class));
        boolean fieldHasClassAnnotation = hasAnnotation(annotatedField.getAnnotations(), ClassRetentionAnnotation.class);
        if (isStaticProvider) {
            assertTrue("Static providers should retain CLASS annotations on fields", fieldHasClassAnnotation);
        }

        List<PConstructor<RetentionAnnotatedClass>> constructors = pClass.getConstructors();
        assertFalse(constructors.isEmpty());
        PConstructor<RetentionAnnotatedClass> primary = constructors.get(0);
        boolean ctorHasClassAnnotation = hasAnnotation(primary.getAnnotations(), ClassRetentionAnnotation.class);
        if (isStaticProvider) {
            assertTrue("Static providers should retain CLASS annotations on constructors", ctorHasClassAnnotation);
        }
    }

    private boolean hasAnnotation(List<PAnnotation> annotations, Class<? extends Annotation> type) {
        for (PAnnotation annotation : annotations) {
            if (type.getName().equals(annotation.getAnnotationClass().getQualifiedName())) {
                return true;
            }
        }
        return false;
    }
}
