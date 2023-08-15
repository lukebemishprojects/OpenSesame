package dev.lukebemish.opensesame.test.otherpackage;

public class HasPrivateCtor {
    private HasPrivateCtor(String arg) {
        System.out.println("ran private constructor: "+arg);
    }
}
