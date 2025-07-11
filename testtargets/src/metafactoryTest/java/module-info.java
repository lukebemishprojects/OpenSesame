import dev.lukebemish.opensesame.test.framework.DelegateEngine;
import org.junit.platform.engine.TestEngine;

open module dev.lukebemish.opensesame.test.metafactory {
    requires dev.lukebemish.opensesame.core;
    requires dev.lukebemish.opensesame.test.target;
    requires org.junit.jupiter.api;
    requires org.objectweb.asm;
    requires java.compiler;

    requires static org.jetbrains.annotations;
    requires org.junit.platform.launcher;

    provides TestEngine with DelegateEngine;
}
