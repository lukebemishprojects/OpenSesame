package dev.lukebemish.opensesame.test.target.hidden;

@SuppressWarnings("unused")
public class Hidden {
    public static String hiddenByModules() {
        return "hiddenByModules";
    }

    private static String hiddenByModulesPrivate() {
        return "hiddenByModulesPrivate";
    }

    private String hiddenByModulesPrivateInstance() {
        return "hiddenByModulesPrivateInstance";
    }
}
