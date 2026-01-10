package space.ruiwang.loadbalance.impl;

import static space.ruiwang.constants.LoadBalancerStrategies.VIRTUAL_NODE_COUNT;

import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;

import cn.hutool.core.util.HashUtil;
import cn.hutool.core.util.IdUtil;
import lombok.extern.slf4j.Slf4j;
import space.ruiwang.domain.ServiceMetaData;
import space.ruiwang.loadbalance.LoadBalancer;
import space.ruiwang.utils.RpcServiceKeyBuilder;

/**
 * @author wangrui <wangrui45@kuaishou.com>
 * Created on 2025-02-19
 */
@Slf4j
public class ConsistentHashingLoadBalancer implements LoadBalancer {
    private final String serviceName;

    private final String serviceVersion;

    private final TreeMap<Long, ServiceMetaData> ring = new TreeMap<>();

    private static final int VIRTUAL_NODE = VIRTUAL_NODE_COUNT;

    public ConsistentHashingLoadBalancer(String serviceName, String serviceVersion) {
        this.serviceName = serviceName;
        this.serviceVersion = serviceVersion;
        log.debug("服务[{}${}]的一致性哈希负载均衡器已构建", serviceName, serviceVersion);
    }

    // 注册服务节点到哈希环
    private void addNode(ServiceMetaData serviceMetaData) {
        String serviceKey = RpcServiceKeyBuilder.buildServiceKey(serviceMetaData.getServiceName(),
                serviceMetaData.getServiceVersion());
        for (int i = 1; i <= VIRTUAL_NODE; i++) {
            long hashKey = hash(serviceKey + "#VN" + i);
            ring.put(hashKey, serviceMetaData);
        }
        log.debug("服务节点已添加到哈希环。节点: [{}], 哈希环: [{}${}]", serviceMetaData, serviceName, serviceVersion);
    }

    //从哈希环移除服务节点
    private void removeNode(ServiceMetaData serviceMetaData) {
        String serviceKey = RpcServiceKeyBuilder.buildServiceKey(serviceMetaData.getServiceName(),
                serviceMetaData.getServiceVersion());
        for (int i = 1; i <= VIRTUAL_NODE; i++) {
            long hashKey = hash(serviceKey + "#VN" + i);
            ring.remove(hashKey);
        }
        log.debug("服务节点已从哈希环删除。节点: [{}], 哈希环: [{}${}]", serviceMetaData, serviceName, serviceVersion);
    }

    // 查找服务节点
    private ServiceMetaData getNode(String key) {
        long hashKey = hash(key);
        Entry<Long, ServiceMetaData> result = ring.ceilingEntry(hashKey);
        if (result == null) {
            return ring.firstEntry().getValue();
        } else {
          return result.getValue();
        }
    }

    /**
     * 对字符串进行哈希
     *
     * @param key 字符串
     * @return hash值
     */
    private int hash(String key) {
        return HashUtil.bkdrHash(key);
    }

    // TODO 每次调用都需要构建哈希环需要优化
    @Override
    public ServiceMetaData selectService(List<ServiceMetaData> availableServices) {
        updateRing(availableServices);
        // 生成一个唯一的key
        String key = generateUniqueKey();
        ServiceMetaData node = getNode(key);
        log.info("服务请求[{}]路由到节点[{}]", key, node);
        return node;
    }

    // 生成唯一key
    private String generateUniqueKey() {
        return serviceName + "$" + serviceVersion + "#" + IdUtil.fastSimpleUUID();
    }

    private void updateRing(List<ServiceMetaData> availableServices) {
        // 移除不在可用列表中的节点
        ring.values().removeIf(service -> !availableServices.contains(service));

        // 添加新的可用节点
        for (ServiceMetaData service : availableServices) {
            if (!ring.containsValue(service)) {
                addNode(service);
            }
        }
    }
}
