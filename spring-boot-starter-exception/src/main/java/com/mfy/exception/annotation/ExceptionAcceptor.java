package com.mfy.exception.annotation;

import java.lang.annotation.*;

/**
 * @author maofangyun
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ExceptionAcceptor {

	Class<? extends Throwable>[] value() default {};

}
