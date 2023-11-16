package dev.lukebemish.opensesame.compile;

public final class Opcodes {
    private Opcodes() {}

    public static final int H_GETFIELD = 1;
    public static final int H_GETSTATIC = 2;
    public static final int H_PUTFIELD = 3;
    public static final int H_PUTSTATIC = 4;
    public static final int H_INVOKEVIRTUAL = 5;
    public static final int H_INVOKESTATIC = 6;
    public static final int H_INVOKESPECIAL = 7;
    public static final int H_NEWINVOKESPECIAL = 8;
    public static final int H_INVOKEINTERFACE = 9;
}
