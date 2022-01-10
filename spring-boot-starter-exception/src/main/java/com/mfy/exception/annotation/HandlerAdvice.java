package com.mfy.exception.annotation;

import org.springframework.stereotype.Component;

import java.lang.annotation.*;

/**
 * @author maofangyun
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface HandlerAdvice {

}
