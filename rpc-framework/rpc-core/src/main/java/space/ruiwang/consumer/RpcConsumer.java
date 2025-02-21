package space.ruiwang.consumer;

import java.util.ServiceLoader;

import space.ruiwang.domain.RpcRequestConfig;
import space.ruiwang.domain.RpcRequestDO;
import space.ruiwang.domain.RpcResponseDO;

/**
 * @author wangrui <wangrui45@kuaishou.com>
 * Created on 2025-02-12
 */
public interface RpcConsumer {

    // 通过 SPI 机制加载 RpcConsumer 的具体实现（例如 tomcat 模块中的 SimpleHttpClient）
    RpcConsumer RPC_CONSUMER = loadRpcConsumer();

    private static RpcConsumer loadRpcConsumer() {
        ServiceLoader<RpcConsumer> loader = ServiceLoader.load(RpcConsumer.class);
        return loader.findFirst()
                .orElseThrow(() -> new RuntimeException("No RpcConsumer implementation found!"));
    }

    RpcResponseDO send(RpcRequestDO rpcRequestDO, RpcRequestConfig rpcRequestConfig);
}
