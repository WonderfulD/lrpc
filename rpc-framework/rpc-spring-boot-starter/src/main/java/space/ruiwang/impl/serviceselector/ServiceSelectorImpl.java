package space.ruiwang.impl.serviceselector;

import java.util.List;

import javax.annotation.Resource;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import space.ruiwang.api.servicediscovery.IServiceDiscovery;
import space.ruiwang.api.serviceselector.IServiceSelector;
import space.ruiwang.domain.RpcRequest;
import space.ruiwang.domain.RpcRequestConfig;
import space.ruiwang.domain.RpcRequestDTO;
import space.ruiwang.domain.ServiceMetaData;


/**
 * @author wangrui <wangrui45@kuaishou.com>
 * Created on 2025-02-20
 */
@Slf4j
@Component
public class ServiceSelectorImpl implements IServiceSelector {
    @Resource
    private IServiceDiscovery serviceDiscovery;
    @Override
    public ServiceMetaData selectService(RpcRequest rpcRequest) {
        RpcRequestDTO requestDTO = rpcRequest.getRequestDTO();
        RpcRequestConfig requestConfig = rpcRequest.getRequestConfig();
        String serviceName = requestDTO.getServiceName();
        String serviceVersion = requestDTO.getServiceVersion();
        String loadBalancerType = requestConfig.getLoadBalancerType();

        try {
            // 获取具体服务
            return serviceDiscovery.selectService(serviceName, serviceVersion, loadBalancerType);
        } catch (Exception e) {
            log.warn("查找可用实例时遇到错误");
            throw e;
        }
    }

    @Override
    public ServiceMetaData selectOtherService(RpcRequest rpcRequest, List<ServiceMetaData> excludedServices) {
        RpcRequestDTO requestDTO = rpcRequest.getRequestDTO();
        RpcRequestConfig requestConfig = rpcRequest.getRequestConfig();
        String serviceName = requestDTO.getServiceName();
        String serviceVersion = requestDTO.getServiceVersion();
        String loadBalancerType = requestConfig.getLoadBalancerType();

        try {
            // 从去掉一些服务实例的列表中获取具体服务
            return serviceDiscovery.selectOtherService(serviceName, serviceVersion, loadBalancerType, excludedServices);
        } catch (Exception e) {
            log.warn("查找其余可用实例时遇到错误");
            throw e;
        }
    }
}
