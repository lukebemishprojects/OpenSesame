package dev.lukebemish.opensesame.test.otherpackage;

public class ToOpen {
    private static String testStatic() {
        return "ran private static method";
    }

    private String testInstance() {
        return "ran private instance method";
    }

    protected String testProtectedInstance() {
        return "ran protected instance method";
    }

    private static String testStaticWithArg(Object object) {
        return "ran private instance method with arg: " + object;
    }

    private static String STATIC = "private static field";
    private String instance = "private instance field";
}
