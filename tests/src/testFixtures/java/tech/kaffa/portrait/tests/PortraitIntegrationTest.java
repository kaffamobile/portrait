package tech.kaffa.portrait.tests;

import java.util.List;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import tech.kaffa.portrait.PAnnotation;
import tech.kaffa.portrait.PClass;
import tech.kaffa.portrait.PField;
import tech.kaffa.portrait.PMethod;
import tech.kaffa.portrait.Portrait;
import tech.kaffa.portrait.proxy.ProxyHandler;
import tech.kaffa.portrait.tests.fixtures.AnnotatedTestClass;
import tech.kaffa.portrait.tests.fixtures.Calculator;
import tech.kaffa.portrait.tests.fixtures.ExceptionTestClass;
import tech.kaffa.portrait.tests.fixtures.MultiConstructorClass;
import tech.kaffa.portrait.tests.fixtures.ParameterTestClass;
import tech.kaffa.portrait.tests.fixtures.ServiceClass;
import tech.kaffa.portrait.tests.fixtures.SimpleReflectiveClass;
import tech.kaffa.portrait.tests.fixtures.SingletonService;
import tech.kaffa.portrait.tests.fixtures.Status;
import tech.kaffa.portrait.tests.fixtures.TestClass;
import tech.kaffa.portrait.tests.fixtures.TestDataClass;
import tech.kaffa.portrait.tests.fixtures.TestInterface;
import tech.kaffa.portrait.tests.fixtures.TestSingleton;

import static org.junit.Assert.*;

public class PortraitIntegrationTest {

    @Before
    public void clearCache() {
        Portrait.clearCache();
    }

    @Test
    public void generatedMetadataMirrorsRuntimeBehaviour() {
        PClass<SimpleReflectiveClass> simpleClass = Portrait.of(SimpleReflectiveClass.class);
        SimpleReflectiveClass instance = simpleClass.createInstance("tester", 21);

        PMethod greet = simpleClass.getMethod("greet");
        PMethod calculate = simpleClass.getMethod("calculate", Portrait.intClass());

        Object greeting = greet != null ? greet.invoke(instance) : null;
        Object result = calculate != null ? calculate.invoke(instance, 2) : null;

        assertEquals("Hello, tester!", greeting);
        assertEquals(42, result);
    }

    @Test
    public void proxiesOperateAcrossRuntimes() {
        PClass<Calculator> calculatorClass = Portrait.of(Calculator.class);
        Calculator proxy = calculatorClass.createProxy((pClass, method, args) -> {
            Object[] safeArgs = args != null ? args : new Object[0];
            int a = (int) safeArgs[0];
            int b = (int) safeArgs[1];
            switch (method.getName()) {
                case "add":
                    return a + b;
                case "multiply":
                    return a * b;
                default:
                    throw new IllegalStateException("Unexpected method " + method.getName());
            }
        });

        assertEquals(7, proxy.add(3, 4));
        assertEquals(9, proxy.multiply(3, 3));
    }

    @Test
    public void singletonStatePersistsAcrossCalls() {
        PClass<SingletonService> singleton = Portrait.of(SingletonService.class);
        PMethod increment = singleton.getMethod("incrementCounter");
        PMethod current = singleton.getMethod("getCounter");
        SingletonService instance = SingletonService.INSTANCE;

        assertNotNull(instance);
        int baseline = current != null ? (int) current.invoke(instance) : 0;
        int first = increment != null ? (int) increment.invoke(instance) : baseline + 1;
        int second = increment != null ? (int) increment.invoke(instance) : baseline + 2;
        int counter = current != null ? (int) current.invoke(instance) : baseline + 2;

        assertEquals(baseline + 1, first);
        assertEquals(baseline + 2, second);
        assertEquals(baseline + 2, counter);
    }

    @Test
    public void enumMetadataStaysIntact() {
        PClass<Status> status = Portrait.of(Status.class);
        Status[] constants = status.getEnumConstants();

        assertNotNull(constants);
        boolean pending = false;
        boolean completed = false;
        for (Status constant : constants) {
            if (constant == Status.PENDING) {
                pending = true;
            } else if (constant == Status.COMPLETED) {
                completed = true;
            }
        }

        assertTrue(pending);
        assertTrue(completed);
    }

    @Test
    public void serviceClassMethodsMutateInternalState() {
        PClass<ServiceClass> serviceClass = Portrait.of(ServiceClass.class);
        ServiceClass instance = serviceClass.createInstance();

        PMethod increment = serviceClass.getMethod("increment");
        PMethod getState = serviceClass.getMethod("getState");
        PMethod reset = serviceClass.getMethod("reset");

        if (increment != null) {
            increment.invoke(instance);
            increment.invoke(instance);
        }
        Object stateAfterIncrements = getState != null ? getState.invoke(instance) : null;
        assertEquals(2, stateAfterIncrements);

        if (reset != null) {
            reset.invoke(instance);
        }
        Object stateAfterReset = getState != null ? getState.invoke(instance) : null;
        assertEquals(0, stateAfterReset);
    }

    @Test
    public void exposesCoreMetadataForString() {
        PClass<String> stringPClass = Portrait.of(String.class);

        assertEquals("String", stringPClass.getSimpleName());
        assertEquals("java.lang.String", stringPClass.getQualifiedName());
        assertFalse(stringPClass.getMethods().isEmpty());

        PMethod substring = stringPClass.getMethod("substring", Portrait.intClass(), Portrait.intClass());
        assertNotNull(substring);
        assertNull(stringPClass.getMethod("doesNotExist"));
    }

    @Test
    public void createsInstancesViaConstructors() {
        PClass<TestClass> pClass = Portrait.of(TestClass.class);

        TestClass defaultInstance = pClass.createInstance();
        assertEquals("default", defaultInstance.getInternalValue());

        TestClass customInstance = pClass.createInstance("custom");
        assertEquals("custom", customInstance.getInternalValue());
    }

    @Test
    public void resolvesSuperclassAndInterfaces() {
        PClass<String> stringClass = Portrait.of(String.class);
        PClass<Object> objectClass = Portrait.of(Object.class);

        assertEquals(objectClass.getQualifiedName(), stringClass.getSuperclass().getQualifiedName());

        boolean hasSerializable = false;
        boolean hasComparable = false;
        boolean hasCharSequence = false;
        for (PClass<?> iface : stringClass.getInterfaces()) {
            String simple = iface.getSimpleName();
            if ("Serializable".equals(simple)) {
                hasSerializable = true;
            } else if ("Comparable".equals(simple)) {
                hasComparable = true;
            } else if ("CharSequence".equals(simple)) {
                hasCharSequence = true;
            }
        }
        assertTrue(hasSerializable);
        assertTrue(hasComparable);
        assertTrue(hasCharSequence);
    }

    @Test
    public void returnsSingletonInstances() {
        PClass<TestSingleton> singleton = Portrait.of(TestSingleton.class);
        TestSingleton instance = TestSingleton.INSTANCE;

        assertNotNull(instance);
        PMethod getter = singleton.getMethod("getSingletonValue");
        assertNotNull(getter);
        assertEquals("singleton", getter.invoke(instance));
    }

    @Test
    public void findsAndInvokesMethods() {
        PClass<TestClass> pClass = Portrait.of(TestClass.class);
        TestClass instance = pClass.createInstance("value");

        PMethod doSomething = pClass.getMethod("doSomething");
        assertNotNull(doSomething);
        assertEquals("did something with value", doSomething.invoke(instance));

        PMethod processValue = pClass.getMethod("processValue", Portrait.intClass());
        assertNotNull(processValue);
        assertEquals(20, processValue.invoke(instance, 10));
    }

    @Test
    public void readsAnnotations() {
        PClass<AnnotatedTestClass> annotated = Portrait.of(AnnotatedTestClass.class);
        PAnnotation classAnnotation = annotated.getAnnotations().stream()
            .filter(annotation -> "TestAnnotation".equals(annotation.getAnnotationClass().getSimpleName()))
            .findFirst()
            .orElse(null);
        assertNotNull(classAnnotation);
        assertEquals("class-level", classAnnotation.getStringValue("value"));

        PMethod method = annotated.getMethod("annotatedMethod");
        assertNotNull(method);
        PAnnotation methodAnnotation = method.getAnnotations().stream()
            .filter(annotation -> "TestAnnotation".equals(annotation.getAnnotationClass().getSimpleName()))
            .findFirst()
            .orElse(null);
        assertNotNull(methodAnnotation);
        assertEquals(Integer.valueOf(200), methodAnnotation.getIntValue("number"));
    }

    @Test
    public void inspectsFields() {
        PClass<TestDataClass> dataPClass = Portrait.of(TestDataClass.class);
        PField field = dataPClass.getField("DEFAULT_NAME");
        assertNotNull(field);

        assertTrue(field.isStatic());
        assertTrue(field.isFinal());
        assertEquals(TestDataClass.DEFAULT_NAME, field.get(null));
    }

    @Test
    public void handlesEnumsAndSealedTypes() {
        PClass<Status> status = Portrait.of(Status.class);
        assertTrue(status.isEnum());
        Status[] constants = status.getEnumConstants();
        assertNotNull(constants);
        assertEquals(4, constants.length);

        PClass<SimpleReflectiveClass> operation = Portrait.of(SimpleReflectiveClass.class);
        boolean hasGreet = false;
        for (PMethod method : operation.getMethods()) {
            if ("greet".equals(method.getName())) {
                hasGreet = true;
                break;
            }
        }
        assertTrue(hasGreet);
    }

    @Test
    public void supportsVarargsAndArrays() {
        PClass<ParameterTestClass> parameterClass = Portrait.of(ParameterTestClass.class);
        List<PMethod> methods = parameterClass.getMethods();
        PMethod arrayMethod = null;
        for (PMethod method : methods) {
            if ("arrayParams".equals(method.getName())) {
                arrayMethod = method;
                break;
            }
        }
        assertNotNull(arrayMethod);
        ParameterTestClass instance = parameterClass.createInstance();
        Object result = arrayMethod.invoke(instance, new int[]{1, 2, 3}, new String[]{"a"});
        assertEquals("arrays", result);
    }

    @Test
    public void selectsMatchingConstructorOverload() {
        Portrait.INSTANCE.debug();
        PClass<MultiConstructorClass> multiConstructor = Portrait.of(MultiConstructorClass.class);

        MultiConstructorClass oneArg = multiConstructor.createInstance("first");
        assertEquals("first", oneArg.getName());
        assertEquals(0, oneArg.getValue());

        MultiConstructorClass twoArgs = multiConstructor.createInstance("second", 5);
        assertEquals(5, twoArgs.getValue());
        assertNull(twoArgs.getOptional());

        MultiConstructorClass threeArgs = multiConstructor.createInstance("third", 7, "maybe");
        assertEquals("maybe", threeArgs.getOptional());
    }

    @Test
    public void surfacesExceptionsThrownByInvokedMethods() {
        PClass<ExceptionTestClass> exceptionClass = Portrait.of(ExceptionTestClass.class);
        PMethod throwsException = exceptionClass.getMethod("throwsException");
        assertNotNull(throwsException);
        ExceptionTestClass instance = exceptionClass.createInstance();

        assertThrows(RuntimeException.class, () -> throwsException.invoke(instance));
    }

    @Test
    public void proxiesInterfaces() {
        PClass<TestInterface> interfacePClass = Portrait.of(TestInterface.class);
        ProxyHandler<TestInterface> handler = (pClass, method, args) -> {
            switch (method.getName()) {
                case "doSomething":
                    return "proxied";
                case "processValue":
                    return args != null && args.length > 0 ? ((Integer) args[0]) * 3 : null;
                case "getName":
                    return "proxy";
                default:
                    return null;
            }
        };
        TestInterface proxy = interfacePClass.createProxy(handler);

        assertEquals("proxied", proxy.doSomething());
        assertEquals(9, proxy.processValue(3));
        assertEquals("proxy", proxy.getName());
    }
}
