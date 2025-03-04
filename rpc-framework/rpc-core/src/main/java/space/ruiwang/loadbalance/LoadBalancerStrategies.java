package space.ruiwang.loadbalance;

/**
 * @author wangrui <wangrui45@kuaishou.com>
 * Created on 2025-02-19
 */
public interface LoadBalancerStrategies {
    String CONSISTENT_HASHING = "space.ruiwang.loadbalance.impl.ConsistentHashingLoadBalancer";
    String RANDOM = "space.ruiwang.loadbalance.impl.RandomLoadBalancer";
}
