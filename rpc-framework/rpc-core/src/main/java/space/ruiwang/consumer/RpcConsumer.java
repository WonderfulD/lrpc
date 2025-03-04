package space.ruiwang.consumer;

import space.ruiwang.domain.RpcRequestConfig;
import space.ruiwang.domain.RpcRequestDO;
import space.ruiwang.domain.RpcResponseDO;

/**
 * @author wangrui <wangrui45@kuaishou.com>
 * Created on 2025-02-12
 */
public interface RpcConsumer {
    RpcResponseDO send(RpcRequestDO rpcRequestDO, RpcRequestConfig rpcRequestConfig);
}
