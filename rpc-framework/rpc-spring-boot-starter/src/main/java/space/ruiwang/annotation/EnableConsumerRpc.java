package space.ruiwang.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Import;

import space.ruiwang.impl.servicediscovery.ServiceFinderImpl;
import space.ruiwang.impl.serviceselector.ServiceSelectorImpl;
import space.ruiwang.loader.RpcConsumerLoader;
import space.ruiwang.loader.ServiceRegisterLoader;
import space.ruiwang.processor.RpcReferenceBeanPostProcessor;
import space.ruiwang.proxy.ProxyAgent;

/**
 * @author wangrui <wangrui45@kuaishou.com>
 * Created on 2025-02-17
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Import({
        RpcReferenceBeanPostProcessor.class,
        RpcConsumerLoader.class,
        ServiceRegisterLoader.class,
        ServiceFinderImpl.class,
        ServiceSelectorImpl.class,
        ProxyAgent.class
})
public @interface EnableConsumerRpc {

}
