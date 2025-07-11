import dev.lukebemish.opensesame.test.framework.DelegateEngine;
import org.junit.platform.engine.TestEngine;

open module dev.lukebemish.opensesame.test.metafactory {
    requires dev.lukebemish.opensesame.core;
    requires dev.lukebemish.opensesame.test.target;
    requires org.junit.jupiter.api;
    requires org.junit.platform.suite.api;
    requires org.objectweb.asm;
    requires org.junit.platform.reporting;
    requires java.compiler;

    requires static org.jetbrains.annotations;

    provides TestEngine with DelegateEngine;
}
