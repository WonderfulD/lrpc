package space.ruiwang.tolerant.impl;

import java.util.List;

import space.ruiwang.domain.RpcRequestConfig;
import space.ruiwang.domain.RpcRequestDTO;
import space.ruiwang.domain.ServiceMetaData;
import space.ruiwang.tolerant.FaultTolerant;

/**
 * @author wangrui <wangrui45@kuaishou.com>
 * Created on 2025-03-08
 */
public class Failsafe implements FaultTolerant {
    @Override
    public void handler(RpcRequestDTO rpcRequestDTO, RpcRequestConfig rpcRequestConfig,
            List<ServiceMetaData> excludedServices, ServiceMetaData service) {
        rpcRequestConfig.setRetryCount(rpcRequestConfig.getRetryCount() - 1);
    }
}
