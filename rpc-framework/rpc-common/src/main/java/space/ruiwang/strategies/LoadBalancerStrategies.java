package space.ruiwang.strategies;

/**
 * @author wangrui <wangrui45@kuaishou.com>
 * Created on 2025-03-10
 */
public interface LoadBalancerStrategies {
    String CONSISTENT_HASHING = "space.ruiwang.loadbalance.impl.ConsistentHashingLoadBalancer";
    String RANDOM = "space.ruiwang.loadbalance.impl.RandomLoadBalancer";
}

