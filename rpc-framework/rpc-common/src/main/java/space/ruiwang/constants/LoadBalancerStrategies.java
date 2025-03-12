package space.ruiwang.constants;

/**
 * @author wangrui <wangrui45@kuaishou.com>
 * Created on 2025-03-10
 */
public class LoadBalancerStrategies {
    public static final String CONSISTENT_HASHING = "space.ruiwang.loadbalance.impl.ConsistentHashingLoadBalancer";
    public static final int VIRTUAL_NODE_COUNT = 100;
    public static final String RANDOM = "space.ruiwang.loadbalance.impl.RandomLoadBalancer";
}

