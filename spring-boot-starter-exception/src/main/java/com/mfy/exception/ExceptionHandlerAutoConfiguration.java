package com.mfy.exception;

import com.mfy.exception.resolver.ExceptionAcceptorExceptionResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * 异常处理器的自动配置类
 * @author maofangyun
 */
@Configuration
@ConditionalOnClass(value = {ExceptionAcceptorExceptionResolver.class})
public class ExceptionHandlerAutoConfiguration {

    @Autowired
    private Environment environment;

    @Bean("exceptionAcceptorExceptionResolver")
    @ConditionalOnMissingBean(ExceptionAcceptorExceptionResolver.class)
    public ExceptionAcceptorExceptionResolver getExceptionResolver(){
        String property = environment.getProperty("rabbitmq.exception.throw");
        ExceptionAcceptorExceptionResolver exceptionResolver = new ExceptionAcceptorExceptionResolver();
        exceptionResolver.setFlag(Boolean.parseBoolean(property));
        return exceptionResolver;
    }

}
