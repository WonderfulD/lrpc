package space.ruiwang.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import space.ruiwang.loadbalance.LoadBalancerStrategies;
import space.ruiwang.tolerant.TolerantStrategies;


/**
 * @author wangrui <wangrui45@kuaishou.com>
 * Created on 2025-02-12
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RpcReference {
    /**
     * 版本
     */
    String serviceVersion() default "1.0";

    /**
     * 负载均衡策略选择
     * 默认为一致性哈希环实现
     */
    String loadBalancer() default LoadBalancerStrategies.CONSISTENT_HASHING;

    /**
     * 超时
     */
    long timeout() default 10000;

    /**
     * 重试机制选择
     * 默认为快速失败
     */
    String tolerant() default TolerantStrategies.FAIL_FAST;

    /**
     * 重试次数
     */
    long retryCount() default 3;
}
