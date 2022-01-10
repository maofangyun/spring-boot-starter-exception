package com.mfy.exception.resolver;

import com.mfy.exception.annotation.ExceptionAcceptor;
import org.springframework.core.ExceptionDepthComparator;
import org.springframework.core.MethodIntrospector;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.util.*;

/**
 * 发现给定类对象的{@link ExceptionAcceptor}注解标注的所有方法,
 * 将异常类型和对应处理方法的映射关系缓存
 * @author maofangyun
 * */
public class ExceptionAcceptorMethodResolver {

    public static final ReflectionUtils.MethodFilter EXCEPTION_ACCEPTOR_METHODS = method ->
            AnnotatedElementUtils.hasAnnotation(method, ExceptionAcceptor.class);

    private final Map<Class<? extends Throwable>, Method> mappedMethods = new HashMap<>(16);

    private final Map<Class<? extends Throwable>, Method> exceptionLookupCache = new ConcurrentReferenceHashMap<>(16);

    public ExceptionAcceptorMethodResolver(Class<?> handlerType) {
        // 获取handlerType中被@ExceptionAcceptor注解的所有方法
        for (Method method : MethodIntrospector.selectMethods(handlerType, EXCEPTION_ACCEPTOR_METHODS)) {
            // 获取method中被@ExceptionAcceptor注解的所有异常属性(Class)
            for (Class<? extends Throwable> exceptionType : detectExceptionMappings(method)) {
                // 建立异常类型和方法的映射关系(mappedMethods)
                this.mappedMethods.put(exceptionType, method);
            }
        }
    }

    /**
     * 根据异常类型,获取对应的处理方法
     * */
    public Method resolveMethod(Exception exception) {
        // 获取异常类型对应的处理方法
        Method method = resolveMethodByExceptionType(exception.getClass());
        if (method == null) {
            Throwable cause = exception.getCause();
            if (cause != null) {
                method = resolveMethodByExceptionType(cause.getClass());
            }
        }
        return method;
    }

    public Method resolveMethodByExceptionType(Class<? extends Throwable> exceptionType) {
        Method method = this.exceptionLookupCache.get(exceptionType);
        if (method == null) {
            // 查找exception类型对应的处理方法
            method = getMappedMethod(exceptionType);
            // 将异常类型和对应的处理方法的映射关系缓存起来
            this.exceptionLookupCache.put(exceptionType, method);
        }
        return method;
    }

    public boolean hasExceptionMappings() {
        return !this.mappedMethods.isEmpty();
    }

    private Method getMappedMethod(Class<? extends Throwable> exceptionType) {
        List<Class<? extends Throwable>> matches = new ArrayList<>();
        for (Class<? extends Throwable> mappedException : this.mappedMethods.keySet()) {
            // 判断业务抛出的异常类型是否属于mappedException
            if (mappedException.isAssignableFrom(exceptionType)) {
                matches.add(mappedException);
            }
        }
        if (!matches.isEmpty()) {
            matches.sort(new ExceptionDepthComparator(exceptionType));
            // 由于业务抛出的异常类型抛出的异常类型,可能对应有多个异常类的处理方法,简单处理,返回第一个
            return this.mappedMethods.get(matches.get(0));
        }
        else {
            return null;
        }
    }

    private List<Class<? extends Throwable>> detectExceptionMappings(Method method) {
        List<Class<? extends Throwable>> result = new ArrayList<>();
        detectAnnotationExceptionMappings(method, result);
        if (result.isEmpty()) {
            for (Class<?> paramType : method.getParameterTypes()) {
                if (Throwable.class.isAssignableFrom(paramType)) {
                    result.add((Class<? extends Throwable>) paramType);
                }
            }
        }
        return result;
    }

    private void detectAnnotationExceptionMappings(Method method, List<Class<? extends Throwable>> result) {
        ExceptionAcceptor ann = AnnotatedElementUtils.findMergedAnnotation(method, ExceptionAcceptor.class);
        result.addAll(Arrays.asList(ann.value()));
    }

}
