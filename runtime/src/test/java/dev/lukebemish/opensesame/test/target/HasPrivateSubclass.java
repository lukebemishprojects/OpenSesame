package dev.lukebemish.opensesame.test.target;

public class HasPrivateSubclass {
    private static class PrivateSubclass {
        private static String invoke() {
            return "PrivateSubclass";
        }

        private final String arg;

        private PrivateSubclass(String arg) {
            this.arg = arg;
        }
    }
}
