package cz.johnslovakia.gameapi.utils.eTrigger;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

//NOT MY CODE

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Condition {

    /**
     * @return {@code true} if the result of condition should be negated
     */
    boolean negate() default false;

    /**
     * Determines if condition should be treated as an alternative one. If one of the non-alternative conditions fails, any passed alternative condition will make the whole test pass.
     * @return {@code true} if the condition should be treated as alternative
     */
    boolean alternative() default false;
}