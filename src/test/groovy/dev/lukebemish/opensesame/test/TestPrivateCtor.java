package dev.lukebemish.opensesame.test;

public class TestPrivateCtor {
    private TestPrivateCtor(String arg) {
        System.out.println("ran private constructor: "+arg);
    }
}
