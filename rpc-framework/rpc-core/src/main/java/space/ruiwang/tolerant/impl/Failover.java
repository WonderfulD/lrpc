package space.ruiwang.tolerant.impl;

import java.util.List;

import space.ruiwang.domain.RpcRequestConfig;
import space.ruiwang.domain.RpcRequestDO;
import space.ruiwang.domain.ServiceRegisterDO;
import space.ruiwang.tolerant.Tolerant;

/**
 * @author wangrui <wangrui45@kuaishou.com>
 * Created on 2025-03-08
 */
public class Failover implements Tolerant {
    @Override
    public void handler(RpcRequestDO rpcRequestDO, RpcRequestConfig rpcRequestConfig,
            List<ServiceRegisterDO> excludedServices, ServiceRegisterDO service) {
        excludedServices.add(service);
        rpcRequestConfig.setRetryCount(rpcRequestConfig.getRetryCount() - 1);
    }
}
