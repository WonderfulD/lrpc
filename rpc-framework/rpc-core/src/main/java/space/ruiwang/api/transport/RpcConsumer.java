package space.ruiwang.api.transport;

import space.ruiwang.domain.RpcRequestConfig;
import space.ruiwang.domain.RpcRequestDO;
import space.ruiwang.domain.RpcResponseDO;
import space.ruiwang.domain.ServiceInstance;

/**
 * @author wangrui <wangrui45@kuaishou.com>
 * Created on 2025-02-12
 */
public interface RpcConsumer {
    RpcResponseDO send(ServiceInstance serviceInstance, RpcRequestDO rpcRequestDO, RpcRequestConfig rpcRequestConfig);
}
