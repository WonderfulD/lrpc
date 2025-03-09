package space.ruiwang.tolerant;

import java.util.List;

import space.ruiwang.domain.RpcRequestConfig;
import space.ruiwang.domain.RpcRequestDO;
import space.ruiwang.domain.ServiceRegisterDO;

/**
 * @author wangrui <wangrui45@kuaishou.com>
 * Created on 2025-02-19
 */
public interface Tolerant {
    void handler(RpcRequestDO rpcRequestDO, RpcRequestConfig rpcRequestConfig,
            List<ServiceRegisterDO> excludedServices, ServiceRegisterDO service);
}
