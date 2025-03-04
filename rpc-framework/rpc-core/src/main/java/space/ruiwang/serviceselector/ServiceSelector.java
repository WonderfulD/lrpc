package space.ruiwang.serviceselector;

import space.ruiwang.domain.RpcRequest;
import space.ruiwang.domain.ServiceInstance;

/**
 * @author wangrui <wangrui45@kuaishou.com>
 * Created on 2025-02-20
 */
public interface ServiceSelector {
    /**
     * 获取具体服务
     * @param rpcRequest
     * @return hostName+port
     */
    ServiceInstance selectService(RpcRequest rpcRequest);
}
