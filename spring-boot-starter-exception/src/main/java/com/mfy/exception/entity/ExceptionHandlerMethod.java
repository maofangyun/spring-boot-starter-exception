package com.mfy.exception.entity;

import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.MethodParameter;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.util.ObjectUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.method.HandlerMethod;

import java.lang.reflect.Method;

/**
 * 异常处理方法调用的封装
 * @author maofangyun
 */
public class ExceptionHandlerMethod extends HandlerMethod {

    private static final Object[] EMPTY_ARGS = new Object[0];

    private ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

    public ExceptionHandlerMethod(Object bean, Method method) {
        super(bean, method);
    }

    public Object doInvoke(Object... args) throws Exception {
        // 异常处理方法的入参赋值
        args = getMethodArgumentValues(args);
        ReflectionUtils.makeAccessible(getBridgedMethod());
        // 反射调用异常处理方法
        return getBridgedMethod().invoke(getBean(), args);
    }

    protected Object[] getMethodArgumentValues(Object... providedArgs){
        MethodParameter[] parameters = getMethodParameters();
        if (ObjectUtils.isEmpty(parameters)) {
            return EMPTY_ARGS;
        }
        Object[] args = new Object[parameters.length];
        for (int i = 0; i < parameters.length; i++) {
            MethodParameter parameter = parameters[i];
            parameter.initParameterNameDiscovery(this.parameterNameDiscoverer);
            // 当提供的参数数组providedArgs中,存在和此方法的入参parameter匹配的参数,直接使用providedArgs中的数据
            args[i] = findProvidedArgument(parameter, providedArgs);
            if (args[i] != null) {
                continue;
            }
        }
        return args;
    }

    private Object findProvidedArgument(MethodParameter parameter, Object[] providedArgs) {
        if (!ObjectUtils.isEmpty(providedArgs)) {
            for (Object providedArg : providedArgs) {
                if (parameter.getParameterType().isInstance(providedArg)) {
                    return providedArg;
                }
            }
        }
        return null;
    }

}
