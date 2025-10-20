package tech.kaffa.portrait.tests.fixtures;

import java.util.Objects;
import tech.kaffa.portrait.ProxyTarget;
import tech.kaffa.portrait.Reflective;

@Reflective
@ProxyTarget
public class TestDataClass {

    public static final String DEFAULT_NAME = "test";

    private final int id;
    private final String name;
    private final boolean active;

    public TestDataClass(int id, String name) {
        this(id, name, true);
    }

    public TestDataClass(int id, String name, boolean active) {
        this.id = id;
        this.name = name;
        this.active = active;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public boolean isActive() {
        return active;
    }

    public TestDataClass updateName(String newName) {
        return new TestDataClass(id, newName, active);
    }

    public static TestDataClass create(int id) {
        return new TestDataClass(id, DEFAULT_NAME);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TestDataClass)) {
            return false;
        }
        TestDataClass that = (TestDataClass) o;
        return id == that.id && active == that.active && Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, active);
    }

    @Override
    public String toString() {
        return "TestDataClass{id=" + id + ", name='" + name + '\'' + ", active=" + active + '}';
    }
}

