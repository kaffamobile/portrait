package tech.kaffa.portrait.tests.fixtures;

import tech.kaffa.portrait.Reflective;

@Reflective
public class PublicVisibilityClass {
    private String value;

    public String publicField = "public";
    protected String protectedField = "protected";
    String packageField = "package";
    private String privateField = "private";

    public PublicVisibilityClass() {
        this("default");
    }

    public PublicVisibilityClass(String value) {
        this.value = value;
    }

    protected PublicVisibilityClass(String value, int hidden) {
        this.value = value + hidden;
    }

    private PublicVisibilityClass(int hidden, boolean flag) {
        this.value = flag ? "hidden" + hidden : "nope";
    }

    public String getValue() {
        return value;
    }

    public String publicMethod() {
        return "public-" + value;
    }

    protected String protectedMethod() {
        return "protected-" + value;
    }

    String packageMethod() {
        return "package-" + value;
    }

    private String privateMethod() {
        return "private-" + value;
    }
}

@Reflective
class PackagePrivateReflectiveClass {
    PackagePrivateReflectiveClass() {
    }
}

final class VisibilityFixtures {
    private VisibilityFixtures() {
    }

    @Reflective
    static class PackagePrivateNestedReflectiveClass {
        PackagePrivateNestedReflectiveClass() {
        }
    }

    @Reflective
    private static class PrivateNestedReflectiveClass {
        private PrivateNestedReflectiveClass() {
        }
    }
}
