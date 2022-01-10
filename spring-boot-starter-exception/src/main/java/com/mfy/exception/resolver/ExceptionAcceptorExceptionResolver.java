package com.mfy.exception.resolver;

import com.mfy.exception.annotation.ExceptionAcceptor;
import com.mfy.exception.entity.ExceptionHandlerMethod;
import com.mfy.exception.entity.HandlerAdviceBean;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.listener.RabbitListenerErrorHandler;
import org.springframework.amqp.rabbit.listener.exception.ListenerExecutionFailedException;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 全局异常处理支持,启动时自动加载解析@HandlerAdvice和@ExceptionAcceptor注解标注的类和方法信息,
 * 将异常类型和对应的处理方法进行绑定
 * 调用{@link ExceptionAcceptor}注解的方法,处理代码抛出的异常
 * @author maofangyun
 * */
@Slf4j
public class ExceptionAcceptorExceptionResolver implements InitializingBean, ApplicationContextAware, RabbitListenerErrorHandler {

    private ApplicationContext applicationContext;

    private boolean flag;

    private final Map<HandlerAdviceBean, ExceptionAcceptorMethodResolver> exceptionAcceptorAdviceCache = new LinkedHashMap<>();

    public void setFlag(boolean flag) {
        this.flag = flag;
    }

    @Override
    public void afterPropertiesSet() {
        initExceptionAcceptorCache();
    }

    private void initExceptionAcceptorCache() {
        // 找出@HandlerAdvice注解的bean
        List<HandlerAdviceBean> adviceBeans = HandlerAdviceBean.findAnnotatedBeans(applicationContext);
        for(HandlerAdviceBean adviceBean : adviceBeans){
            Class<?> beanType = adviceBean.getBeanType();
            // 建立异常类型和方法的映射关系(mappedMethods)
            ExceptionAcceptorMethodResolver resolver = new ExceptionAcceptorMethodResolver(beanType);
            if (resolver.hasExceptionMappings()) {
                // 建立异常处理类和处理方法封装对象的映射关系
                this.exceptionAcceptorAdviceCache.put(adviceBean, resolver);
            }
        }
    }

    public void doResolveException(Exception exception) {
        ExceptionHandlerMethod handlerMethod = getExceptionAcceptorMethod(exception);
        Throwable cause = exception.getCause();
        try {
            if(cause != null){
                handlerMethod.doInvoke(exception,cause);
            } else {
                handlerMethod.doInvoke(exception);
            }
        } catch (Throwable invocationEx) {
            if (invocationEx != exception && invocationEx != exception.getCause() && log.isWarnEnabled()) {
                log.warn("Failure in @ExceptionAcceptor " + handlerMethod, invocationEx);
            }
        }
    }

    protected ExceptionHandlerMethod getExceptionAcceptorMethod(Exception exception){
        for (Map.Entry<HandlerAdviceBean, ExceptionAcceptorMethodResolver> entry : this.exceptionAcceptorAdviceCache.entrySet()) {
            HandlerAdviceBean advice = entry.getKey();
            ExceptionAcceptorMethodResolver resolver = entry.getValue();
            // 获取异常类型对应的处理方法
            Method method = resolver.resolveMethod(exception);
            if (method != null) {
                // 将实例bean和需要调用的method封装在一起,方便后面的反射调用
                return new ExceptionHandlerMethod(advice.resolveBean(), method);
            }
        }
        return null;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public Object handleError(Message amqpMessage, org.springframework.messaging.Message<?> message, ListenerExecutionFailedException exception) throws Exception {
        Exception exceptionCause = (Exception) exception.getCause();
        doResolveException(exceptionCause);
        if(flag){
            throw exceptionCause;
        }
        return null;
    }
}
