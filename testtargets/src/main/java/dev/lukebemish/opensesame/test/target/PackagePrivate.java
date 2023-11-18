package dev.lukebemish.opensesame.test.target;

@SuppressWarnings("unused")
public class PackagePrivate {
    private String privateInstance() {
        return "privateInstance";
    }

    @Override
    public String toString() {
        return "PackagePrivate";
    }
}
