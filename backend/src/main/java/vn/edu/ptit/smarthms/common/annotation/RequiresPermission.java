package vn.edu.ptit.smarthms.common.annotation;

import java.lang.annotation.*;

/**
 * Method-level annotation to enforce fine-grained permission checks via AOP.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequiresPermission {
    String resource();
    String action();
    String scope() default "ANY";
}
