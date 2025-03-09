package space.ruiwang.serviceselector.impl;

import java.util.List;

import javax.annotation.Resource;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import space.ruiwang.domain.RpcRequest;
import space.ruiwang.domain.RpcRequestConfig;
import space.ruiwang.domain.RpcRequestDO;
import space.ruiwang.domain.ServiceRegisterDO;
import space.ruiwang.servicefinder.IServiceFinder;
import space.ruiwang.serviceselector.IServiceSelector;

/**
 * @author wangrui <wangrui45@kuaishou.com>
 * Created on 2025-02-20
 */
@Slf4j
@Component
public class ServiceSelectorImpl implements IServiceSelector {
    @Resource
    private IServiceFinder serviceFinder;
    @Override
    public ServiceRegisterDO selectService(RpcRequest rpcRequest) {
        RpcRequestDO requestDO = rpcRequest.getRequestDO();
        RpcRequestConfig requestConfig = rpcRequest.getRequestConfig();
        String serviceName = requestDO.getServiceName();
        String serviceVersion = requestDO.getServiceVersion();
        String loadBalancerType = requestConfig.getLoadBalancerType();

        try {
            // 获取具体服务
            return serviceFinder.selectService(serviceName, serviceVersion, loadBalancerType);
        } catch (Exception e) {
            throw new RuntimeException("Rpc调用失败，无法找到实例");
        }
    }

    @Override
    public ServiceRegisterDO selectOtherService(RpcRequest rpcRequest, List<ServiceRegisterDO> excludedServices) {
        RpcRequestDO requestDO = rpcRequest.getRequestDO();
        RpcRequestConfig requestConfig = rpcRequest.getRequestConfig();
        String serviceName = requestDO.getServiceName();
        String serviceVersion = requestDO.getServiceVersion();
        String loadBalancerType = requestConfig.getLoadBalancerType();

        try {
            // 从去掉一些服务实例的列表中获取具体服务
            return serviceFinder.selectOtherService(serviceName, serviceVersion, loadBalancerType, excludedServices);
        } catch (Exception e) {
            throw new RuntimeException("Rpc调用失败，无法找到实例");
        }
    }
}
