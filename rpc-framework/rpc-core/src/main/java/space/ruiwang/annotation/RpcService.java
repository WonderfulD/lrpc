package space.ruiwang.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.stereotype.Component;

/**
 * @author wangrui <wangrui45@kuaishou.com>
 * Created on 2025-02-12
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Component
public @interface RpcService {
    /**
     * 指定实现接口，默认为实现的第一个接口
     */
    Class<?> service() default void.class;

    /**
     * 版本
     */
    String serviceVersion() default "1.0";

    /**
     * 服务有效期
     */
    long ttl() default 300000L;
}
