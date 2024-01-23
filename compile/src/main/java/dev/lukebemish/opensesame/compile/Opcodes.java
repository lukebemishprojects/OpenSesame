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

    public static final int ACC_PUBLIC = 0x0001;
    public static final int ACC_STATIC = 0x0008;
    public static final int ACC_FINAL = 0x0010;
    public static final int ACC_SYNTHETIC = 0x1000;

    public static final int GETSTATIC = 178;
    public static final int DUP = 89;
    public static final int MONITORENTER = 194;
    public static final int MONITOREXIT = 195;
    public static final int ICONST_0 = 3;
    public static final int ALOAD = 25;
    public static final int ARETURN = 176;
    public static final int RETURN = 177;
    public static final int INVOKESTATIC = 184;
    public static final int NEW = 187;
    public static final int INVOKESPECIAL = 183;
    public static final int INVOKEINTERFACE = 185;
    public static final int POP = 87;
    public static final int AASTORE = 83;
    public static final int AALOAD = 50;
    public static final int ICONST_1 = 4;
    public static final int ANEWARRAY = 189;
    public static final int PUTSTATIC = 179;
}
