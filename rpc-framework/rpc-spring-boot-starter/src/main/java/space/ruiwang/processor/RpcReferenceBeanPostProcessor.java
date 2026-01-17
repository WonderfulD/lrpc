package space.ruiwang.processor;

import java.lang.reflect.Field;

import javax.annotation.Resource;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotatedElementUtils;

import lombok.extern.slf4j.Slf4j;
import space.ruiwang.annotation.RpcAgentReference;
import space.ruiwang.annotation.RpcReference;
import space.ruiwang.domain.RpcRequestConfig;
import space.ruiwang.proxy.ProxyAgent;

/**
 * 处理 @RpcReference 注解的 BeanPostProcessor
 */
@Slf4j
@Configuration
public class RpcReferenceBeanPostProcessor implements BeanPostProcessor {
    @Resource
    private ProxyAgent proxyFactory;

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        Class<?> beanClass = bean.getClass();
        // 检查类中的字段是否有 @RpcReference 注解
        for (Field field : beanClass.getDeclaredFields()) {
            RpcAgentReference agentReference = AnnotatedElementUtils.findMergedAnnotation(field, RpcAgentReference.class);
            RpcReference rpcReference = AnnotatedElementUtils.findMergedAnnotation(field, RpcReference.class);
            if (agentReference != null) {
                Object proxy = proxyFactory.getAgentInvokeProxy(field.getType());
                field.setAccessible(true);
                try {
                    field.set(bean, proxy);
                    log.info("field: {} 注入成功，rpcAgentReference参数：{}", field.getName(), agentReference);
                } catch (IllegalAccessException e) {
                    log.error("field: {} 注入失败", field.getName(), e);
                }
                continue;
            }
            if (rpcReference != null) {
                String serviceVersion = rpcReference.serviceVersion();
                String loadBalancerType = rpcReference.loadBalancer();
                long retryCount = rpcReference.retryCount();
                long timeout = rpcReference.timeout();
                String tolerant = rpcReference.tolerant();
                RpcRequestConfig rpcRequestConfig = new RpcRequestConfig(loadBalancerType, retryCount, timeout, tolerant);

                // 获取代理对象并注入
                Object proxy = proxyFactory.getProxy(field.getType(), serviceVersion, rpcRequestConfig);
                field.setAccessible(true);
                try {
                    field.set(bean, proxy);
                    log.info("field: {} 注入成功，rpcReference参数：{}", field.getName(), rpcReference);
                } catch (IllegalAccessException e) {
                    log.error("field: {} 注入失败", field.getName(), e);
                }
            }
        }
        return bean;
    }
}
