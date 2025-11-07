package com.coding.cz.recon.util;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * @description <>
 * @author: zhouchaoyu
 * @Date: 2025-10-30
 */
@Component
public class SpringContextHolder implements ApplicationContextAware {
    private static ApplicationContext applicationContext;

    // Spring容器初始化时会自动调用该方法，注入上下文
    @Override
    public void setApplicationContext(ApplicationContext context) throws BeansException {
        applicationContext = context;
    }

    // 获取Bean
    public static <T> T getBean(Class<T> clazz) {
        if (applicationContext == null) {
            throw new RuntimeException("SpringContextHolder未初始化，applicationContext为null");
        }
        return applicationContext.getBean(clazz);
    }
}