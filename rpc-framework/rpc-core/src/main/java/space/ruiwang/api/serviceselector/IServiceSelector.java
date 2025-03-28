package space.ruiwang.api.serviceselector;

import java.util.List;

import space.ruiwang.domain.RpcRequest;
import space.ruiwang.domain.ServiceMetaData;

/**
 * @author wangrui <wangrui45@kuaishou.com>
 * Created on 2025-02-20
 */
public interface IServiceSelector {
    /**
     * 获取具体服务
     * @param rpcRequest rpc请求
     * @return hostName+port
     */
    ServiceMetaData selectService(RpcRequest rpcRequest);
    ServiceMetaData selectOtherService(RpcRequest rpcRequest, List<ServiceMetaData> excludedServices);
}
