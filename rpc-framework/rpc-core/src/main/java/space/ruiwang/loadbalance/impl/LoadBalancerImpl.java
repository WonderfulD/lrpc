package space.ruiwang.loadbalance.impl;

import java.util.List;

import space.ruiwang.domain.ServiceRegisterDO;
import space.ruiwang.loadbalance.LoadBalancer;

/**
 * @author wangrui <wangrui45@kuaishou.com>
 * Created on 2025-02-11
 */
public class LoadBalancerImpl implements LoadBalancer {
    @Override
    public ServiceRegisterDO selectService(List<ServiceRegisterDO> registeredServices) {
        // 从registeredServices中随机选择一个服务
        int index = (int) (Math.random() * registeredServices.size());
        return registeredServices.get(index);
    }
}
