package tech.kaffa.portrait.tests;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;
import tech.kaffa.portrait.PClass;
import tech.kaffa.portrait.PConstructor;
import tech.kaffa.portrait.PField;
import tech.kaffa.portrait.PMethod;
import tech.kaffa.portrait.Portrait;
import tech.kaffa.portrait.PortraitNotFoundException;
import tech.kaffa.portrait.tests.fixtures.PublicVisibilityClass;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;

public class PublicApiVisibilityTest {

    @Before
    public void clearCache() {
        Portrait.clearCache();
    }

    @Test
    public void onlyPublicMembersAreAccessible() {
        PClass<PublicVisibilityClass> pClass = Portrait.of(PublicVisibilityClass.class);

        List<PConstructor<PublicVisibilityClass>> constructors = pClass.getConstructors();
        assertEquals(2, constructors.size());

        PClass<String> stringClass = Portrait.of(String.class);
        assertNotNull(pClass.getConstructor());
        assertNotNull(pClass.getConstructor(stringClass));
        assertNull(pClass.getConstructor(stringClass, Portrait.intClass()));
        assertNull(pClass.getConstructor(Portrait.intClass(), Portrait.booleanClass()));

        PField publicField = pClass.getField("publicField");
        assertNotNull(publicField);
        assertNull(pClass.getField("protectedField"));
        assertNull(pClass.getField("packageField"));
        assertNull(pClass.getField("privateField"));

        List<String> fieldNames = pClass.getFields().stream()
            .map(PField::getName)
            .collect(Collectors.toList());
        assertEquals(List.of("publicField"), fieldNames);

        assertNotNull(pClass.getMethod("publicMethod"));
        assertNotNull(pClass.getMethod("getValue"));
        assertNull(pClass.getMethod("protectedMethod"));
        assertNull(pClass.getMethod("packageMethod"));
        assertNull(pClass.getMethod("privateMethod"));

        List<String> methodNames = pClass.getMethods().stream()
            .map(PMethod::getName)
            .sorted()
            .collect(Collectors.toList());
        assertEquals(List.of("getValue", "publicMethod"), methodNames);
    }

    @Test
    public void nonPublicClassesAreRejected() {
        assertThrows(
            PortraitNotFoundException.class,
            () -> Portrait.forName("tech.kaffa.portrait.tests.fixtures.PackagePrivateReflectiveClass")
        );

        assertThrows(
            PortraitNotFoundException.class,
            () -> Portrait.forName("tech.kaffa.portrait.tests.fixtures.VisibilityFixtures$PackagePrivateNestedReflectiveClass")
        );

        assertThrows(
            PortraitNotFoundException.class,
            () -> Portrait.forName("tech.kaffa.portrait.tests.fixtures.VisibilityFixtures$PrivateNestedReflectiveClass")
        );
    }
}
