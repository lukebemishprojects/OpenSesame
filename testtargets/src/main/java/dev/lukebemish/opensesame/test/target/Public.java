package dev.lukebemish.opensesame.test.target;

@SuppressWarnings("unused")
public class Public {
    private String privateInstance() {
        return "privateInstance";
    }

    // Test that overloaded private method accesses still happen on the targeted method
    private String privateInstanceOverloaded() {
        return "privateInstanceOverloaded";
    }

    protected String protectedInstance() {
        return "protectedInstance";
    }

    String packagePrivateInstance() {
        return "packagePrivateInstance";
    }

    String packagePrivateInstanceField;
    private String privateInstanceField;
    protected String protectedInstanceField;

    private final String privateFinalInstanceField = "privateFinalInstanceField";

    private static String privateStatic() {
        return "privateStatic";
    }

    protected static String protectedStatic() {
        return "protectedStatic";
    }

    static String packagePrivateStatic() {
        return "packagePrivateStatic";
    }

    private static String privateStaticField;
    protected static String protectedStaticField;
    static String packagePrivateStaticField;

    private static final String privateFinalStaticField = "privateFinalStaticField";

    private static class Private {
        private String privateInstance() {
            return "privateInstance";
        }

        @Override
        public String toString() {
            return "Private";
        }
    }

    public static class PrivateCtor {
        private PrivateCtor() {}

        @Override
        public String toString() {
            return "PrivateCtor";
        }
    }

    public static class PublicSubclass extends Public {
        public String privateInstanceOverloaded() {
            return "overload";
        }
    }

    public static int voidReturnCounter = 0;
    private static String simpleReturn() {
        return "simpleReturn";
    }
    private static void voidReturn() {
        voidReturnCounter++;
    }

    private static int primitiveReturn() {
        return 5;
    }
    private static Integer boxedReturn() {
        return 5;
    }
    private static int[] primitiveArrayReturn() {
        return new int[] {1, 2};
    }

    private static Private privateReturn() {
        return new Private();
    }

    private static String[] arrayReturn() {
        return new String[] {"a", "b"};
    }

    private static String simpleArgument(String s) {
        return s;
    }
    private static String primitiveArgument(int i) {
        return i + "";
    }
    private static String boxedArgument(Integer i) {
        return i + "";
    }
    private static String primitiveArrayArgument(int[] i) {
        return i[0] + "";
    }

    private static String privateArgument(Private p) {
        return p.toString() + "1";
    }

    private static String arrayArgument(String[] s) {
        return s[0];
    }

    public final String finalMethod() {
        return "finalMethod";
    }
}
