package space.ruiwang.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Import;

import space.ruiwang.processor.RpcServiceBeanPostProcessor;


/**
 * @author wangrui <wangrui45@kuaishou.com>
 * Created on 2025-02-17
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Import(RpcServiceBeanPostProcessor.class)
public @interface EnableProviderRpc {

}
