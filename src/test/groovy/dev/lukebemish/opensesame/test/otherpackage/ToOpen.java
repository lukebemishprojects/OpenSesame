package dev.lukebemish.opensesame.test.otherpackage;

public class ToOpen {
    private static void testStatic() {
        System.out.println("ran private static method");
    }

    private void testInstance() {
        System.out.println("ran private instance method");
    }

    protected void testProtectedInstance() {
        System.out.println("ran protected instance method");
    }

    private static void testStaticWithArg(Object object) {
        System.out.println("ran private instance method with arg: " + object);
    }

    private static String STATIC = "private static field";
    private String instance = "private instance field";
}
