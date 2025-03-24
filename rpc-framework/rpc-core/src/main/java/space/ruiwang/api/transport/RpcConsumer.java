package space.ruiwang.api.transport;

import space.ruiwang.domain.RpcRequestConfig;
import space.ruiwang.domain.RpcRequestDTO;
import space.ruiwang.domain.RpcResponseDTO;
import space.ruiwang.domain.ServiceInstance;

/**
 * @author wangrui <wangrui45@kuaishou.com>
 * Created on 2025-02-12
 */
public interface RpcConsumer {
    RpcResponseDTO send(ServiceInstance serviceInstance, RpcRequestDTO rpcRequestDTO, RpcRequestConfig rpcRequestConfig);
}
