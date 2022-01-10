package com.mfy.exception.entity;

import com.mfy.exception.annotation.HandlerAdvice;
import lombok.EqualsAndHashCode;
import org.springframework.aop.scope.ScopedProxyUtils;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.util.ClassUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * @HandlerAdvice 注解的信息封装
 * */
@EqualsAndHashCode
public class HandlerAdviceBean {

    private final String beanName;

    private final Class<?> beanType;

    private final BeanFactory beanFactory;

    public HandlerAdviceBean(String beanName, BeanFactory beanFactory){
        this.beanName = beanName;
        this.beanType = getBeanType(beanName, beanFactory);
        this.beanFactory = beanFactory;
    }

    public static List<HandlerAdviceBean> findAnnotatedBeans(ApplicationContext context) {
        List<HandlerAdviceBean> adviceBeans = new ArrayList<>();
        // 查找beanFactory中所有的bean
        for (String name : BeanFactoryUtils.beanNamesForTypeIncludingAncestors(context, Object.class)) {
            // 排除动态生成的代理类型
            if (!ScopedProxyUtils.isScopedTarget(name)) {
                HandlerAdvice controllerAdvice = context.findAnnotationOnBean(name, HandlerAdvice.class);
                if (controllerAdvice != null) {
                    adviceBeans.add(new HandlerAdviceBean(name, context));
                }
            }
        }
        return adviceBeans;
    }

    public Class<?> getBeanType() {
        return this.beanType;
    }

    private static Class<?> getBeanType(String beanName, BeanFactory beanFactory) {
        Class<?> beanType = beanFactory.getType(beanName);
        return (beanType != null ? ClassUtils.getUserClass(beanType) : null);
    }

    public Object resolveBean() {
        Object bean = beanFactory.getBean(beanName);
        return bean;
    }
}
