package space.ruiwang.tolerant;

import space.ruiwang.domain.RpcRequest;

/**
 * @author wangrui <wangrui45@kuaishou.com>
 * Created on 2025-02-19
 */
public interface Tolerent {
    void handler(RpcRequest rpcRequest);
}
