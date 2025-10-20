package tech.kaffa.portrait.tests.fixtures;

public final class TestUtils {

    private TestUtils() {
    }

    public static TestDataClass createTestDataClass() {
        return createTestDataClass(1, "test");
    }

    public static TestDataClass createTestDataClass(int id, String name) {
        return new TestDataClass(id, name);
    }

    public static TestClass createTestClass() {
        return createTestClass("test");
    }

    public static TestClass createTestClass(String value) {
        return new TestClass(value);
    }
}

