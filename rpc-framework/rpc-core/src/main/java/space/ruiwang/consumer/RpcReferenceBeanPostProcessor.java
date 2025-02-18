package space.ruiwang.consumer;

import java.lang.reflect.Field;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Configuration;

import lombok.extern.slf4j.Slf4j;
import space.ruiwang.annotation.RpcReference;
import space.ruiwang.proxy.ProxyFactory;

/**
 * 处理 @RpcReference 注解的 BeanPostProcessor
 */
@Slf4j
@Configuration
public class RpcReferenceBeanPostProcessor implements BeanPostProcessor {

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        Class<?> beanClass = bean.getClass();
        // 检查类中的字段是否有 @RpcReference 注解
        for (Field field : beanClass.getDeclaredFields()) {
            if (field.isAnnotationPresent(RpcReference.class)) {
                // 获取代理对象并注入
                Object proxy = ProxyFactory.getProxy(field.getType());
                field.setAccessible(true);
                try {
                    field.set(bean, proxy);
                    log.info("field: {} 注入成功", field.getName());
                } catch (IllegalAccessException e) {
                    log.error("field: {} 注入失败", field.getName(), e);
                }
            }
        }
        return bean;
    }
}