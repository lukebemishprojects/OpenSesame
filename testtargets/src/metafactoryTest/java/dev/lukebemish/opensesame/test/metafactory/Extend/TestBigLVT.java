package dev.lukebemish.opensesame.test.metafactory.Extend;

import dev.lukebemish.opensesame.annotations.Open;
import dev.lukebemish.opensesame.annotations.extend.Constructor;
import dev.lukebemish.opensesame.annotations.extend.Extend;
import dev.lukebemish.opensesame.annotations.extend.Field;
import dev.lukebemish.opensesame.annotations.extend.Overrides;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestBigLVT {
    @Open(
            name = "toString",
            targetName = "dev.lukebemish.opensesame.test.target.BigLVT$Inner",
            type = Open.Type.SPECIAL
    )
    static String superToString(Object instance) {
        throw new AssertionError("Method not transformed");
    }

    @Extend(
            targetName = "dev.lukebemish.opensesame.test.target.BigLVT$Inner",
            unsafe = true
    )
    public interface Simple {
        @Constructor
        static Simple constructor(long l1, String s1, long l2, String s2) {
            throw new AssertionError("Constructor not transformed");
        }
    }
    @Extend(
            targetName = "dev.lukebemish.opensesame.test.target.BigLVT$Inner",
            unsafe = true
    )
    public interface AddField {
        @Constructor
        static AddField constructor(
                @Field(value = "text") String text,
                long l1, String s1, long l2, String s2
        ) {
            throw new AssertionError("Constructor not transformed");
        }

        @Field(value = "text")
        String getText();

        @Overrides(value = "toString")
        default String toStringImplementation() {
            return getText() + superToString(this);
        }
    }
    @Extend(
            targetName = "dev.lukebemish.opensesame.test.target.BigLVT$Inner",
            unsafe = true
    )
    public interface AddBigField {
        @Constructor
        static AddBigField constructor(
                @Field(value = "value") long value,
                long l1, String s1, long l2, String s2
        ) {
            throw new AssertionError("Constructor not transformed");
        }

        @Field(value = "value")
        long getValue();

        @Overrides(value = "toString")
        default String toStringImplementation() {
            return getValue() + superToString(this);
        }
    }

    @Test
    void simple() {
        assertEquals("1234", Simple.constructor(1, "2", 3, "4").toString());
    }

    @Test
    void shortField() {
        assertEquals("12341234", AddField.constructor("1234", 1, "2", 3, "4").toString());
    }

    @Test
    void longField() {
        assertEquals("51234", AddBigField.constructor(5, 1, "2", 3, "4").toString());
    }
}
