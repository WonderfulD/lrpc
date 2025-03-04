package space.ruiwang.loadbalance.impl;

import java.util.List;

import lombok.extern.slf4j.Slf4j;
import space.ruiwang.domain.ServiceRegisterDO;
import space.ruiwang.loadbalance.LoadBalancer;

/**
 * @author wangrui <wangrui45@kuaishou.com>
 * Created on 2025-02-11
 */
@Slf4j
public class RandomLoadBalancer implements LoadBalancer {
    private final String serviceName;
    private final String serviceVersion;

    public RandomLoadBalancer(String serviceName, String serviceVersion) {
        this.serviceName = serviceName;
        this.serviceVersion = serviceVersion;
        log.info("服务[{}${}]的随机负载均衡器已构建", serviceName, serviceVersion);
    }

    @Override
    public ServiceRegisterDO selectService(List<ServiceRegisterDO> registeredServices) {
        if (registeredServices == null || registeredServices.isEmpty()) {
            throw new RuntimeException("无可用实例");
        }
        // 从registeredServices中随机选择一个服务
        int index = (int) (Math.random() * registeredServices.size());
        return registeredServices.get(index);
    }
}
