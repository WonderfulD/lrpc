package space.ruiwang.tolerant;

import java.util.List;

import space.ruiwang.domain.RpcRequestConfig;
import space.ruiwang.domain.RpcRequestDTO;
import space.ruiwang.domain.ServiceMetaData;

/**
 * @author wangrui <wangrui45@kuaishou.com>
 * Created on 2025-03-10
 */
public interface FaultTolerant {
    void handler(RpcRequestDTO rpcRequestDTO, RpcRequestConfig rpcRequestConfig,
            List<ServiceMetaData> excludedServices, ServiceMetaData service);
}
