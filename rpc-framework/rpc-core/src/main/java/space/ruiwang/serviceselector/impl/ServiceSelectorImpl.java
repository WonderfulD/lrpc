package space.ruiwang.serviceselector.impl;

import javax.annotation.Resource;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import space.ruiwang.domain.RpcRequest;
import space.ruiwang.domain.RpcRequestConfig;
import space.ruiwang.domain.RpcRequestDO;
import space.ruiwang.domain.ServiceInstance;
import space.ruiwang.domain.ServiceRegisterDO;
import space.ruiwang.servicefinder.ServiceFinder;
import space.ruiwang.serviceselector.ServiceSelector;

/**
 * @author wangrui <wangrui45@kuaishou.com>
 * Created on 2025-02-20
 */
@Slf4j
@Component
public class ServiceSelectorImpl implements ServiceSelector {
    @Resource
    private ServiceFinder serviceFinder;
    @Override
    public ServiceInstance selectService(RpcRequest rpcRequest) {
        RpcRequestDO requestDO = rpcRequest.getRequestDO();
        RpcRequestConfig requestConfig = rpcRequest.getRequestConfig();
        String serviceName = requestDO.getServiceName();
        String serviceVersion = requestDO.getServiceVersion();
        String loadBalancerType = requestConfig.getLoadBalancerType();

        try {
            // 获取具体服务
            ServiceRegisterDO selectedService =
                    serviceFinder.selectService(serviceName, serviceVersion, loadBalancerType);
            String hostName = selectedService.getServiceAddr();
            int port = selectedService.getPort();
            return new ServiceInstance(hostName, port);
        } catch (Exception e) {
            // TODO 结合重试机制抛出有message的Exception
            log.error("rpc请求失败，错误信息:[{}]", e.getMessage());
            throw new RuntimeException("Rpc调用失败，无法找到实例");
        }
    }
}
