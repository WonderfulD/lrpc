package space.ruiwang.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author wangrui <wangrui45@kuaishou.com>
 * Created on 2025-02-12
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface RpcService {
    /**
     * 指定实现接口，默认为实现的第一个接口
     * @return
     */
    Class<?> value() default void.class;

    /**
     * 版本
     * @return
     */
    String serviceVersion() default "1.0";
}
