package dev.lukebemish.opensesame.test.target;

public class BigLVT {
    private static long longReturn() {
        return 3L;
    }

    private static String longArgument(long l) {
        return Long.toString(l);
    }

    private static String longArgument(long l, long l2) {
        return l + Long.toString(l2);
    }

    private static String longMixedArguments(long l, String s, long l2) {
        return l + s + l2;
    }

    private static String longCenterArgument(String s, long l, String s2) {
        return s + l + s2;
    }

    private static double doubleReturn() {
        return 3.0;
    }

    private static String doubleArgument(double d) {
        return Integer.toString((int) Math.floor(d));
    }

    private static String doubleArgument(double d, double d2) {
        return (int) Math.floor(d) + Integer.toString((int) Math.floor(d2));
    }

    private static String doubleMixedArguments(double d, String s, double d2) {
        return (int) Math.floor(d) + s + (int) Math.floor(d2);
    }

    private static String doubleCenterArgument(String s, double d, String s2) {
        return s + (int) Math.floor(d) + s2;
    }
}
