package space.ruiwang.impl.servicediscovery;

import java.lang.reflect.Constructor;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import cn.hutool.core.collection.CollUtil;
import lombok.extern.slf4j.Slf4j;
import space.ruiwang.api.servicediscovery.IServiceDiscovery;
import space.ruiwang.api.serviceregister.sub.ILocalServiceRegister;
import space.ruiwang.api.serviceregister.sub.IRemoteServiceRegister;
import space.ruiwang.domain.ServiceMetaData;
import space.ruiwang.exception.NoAvailInstanceException;
import space.ruiwang.loadbalance.LoadBalancer;
import space.ruiwang.loadbalance.impl.RandomLoadBalancer;
import space.ruiwang.utils.RpcServiceKeyBuilder;

/**
 * @author wangrui <wangrui45@kuaishou.com>
 * Created on 2025-02-11
 */
@Slf4j
@Component
public class ServiceFinderImpl implements IServiceDiscovery {

    @Autowired
    private ILocalServiceRegister localServiceRegister;

    @Autowired
    private IRemoteServiceRegister remoteServiceRegister;

    /**
     * 根据服务名和服务版本号查找具体执行方法的服务
     * @param serviceName 服务名
     * @param serviceVersion 服务版本号
     * @return 具体执行方法的服务，找不到则返回null
     */
    @Override
    public ServiceMetaData selectService(String serviceName, String serviceVersion, String loadBalancerType) {
        List<ServiceMetaData> allAvailableServices = getAllAvailableServices(serviceName, serviceVersion);
        return getService(serviceName, serviceVersion, allAvailableServices, loadBalancerType);
    }

    /**
     * 根据服务名和服务版本号查找具体执行方法的服务，去掉传入的服务实例列表
     */
    @Override
    public ServiceMetaData selectOtherService(String serviceName, String serviceVersion, String loadBalancerType,
            List<ServiceMetaData> excludedServices) {
        if (CollUtil.isEmpty(excludedServices)) {
            return selectService(serviceName, serviceVersion, loadBalancerType);
        }
        List<ServiceMetaData> allAvailableServices = getAllAvailableServices(serviceName, serviceVersion);
        List<ServiceMetaData> filtered =
                allAvailableServices.stream().filter(e -> !excludedServices.contains(e)).collect(Collectors.toList());
        return getService(serviceName, serviceVersion, filtered, loadBalancerType);
    }

    /**
     * 查找服务：先查找本地注册中心，如果找不到再查找远程注册中心
     * @param serviceName 服务名
     * @param serviceVersion 服务版本号
     * @return 服务对应的 Class 列表，找不到则返回null
     */
    @Override
    public List<ServiceMetaData> getAllAvailableServices(String serviceName, String serviceVersion) {
        // 1. 先从本地注册中心查找
        String serviceKey = RpcServiceKeyBuilder.buildServiceKey(serviceName, serviceVersion);
        List<ServiceMetaData> localServices = localServiceRegister.search(serviceKey);
        if (CollUtil.isNotEmpty(localServices)) {
            // 本地可用服务实例列表非空
            log.info("本地注册中心找到可用服务实例列表 [{}], 返回 已登记服务 列表: {}", serviceKey, localServices);
            return localServices;
        }

        // 2. 本地没有，去远程注册中心查找
        log.warn("本地注册中心未找到可用服务实例列表 [{}], 正在尝试从远程注册中心查找...", serviceKey);
        List<ServiceMetaData> remoteServices = remoteServiceRegister.search(serviceKey);
        if (CollUtil.isNotEmpty(remoteServices)) {
            // 远程可用服务实例列表非空
            log.info("远程注册中心找到可用服务 [{}], 返回 已登记服务 列表: {}", serviceKey, remoteServices);
            // 将远程服务同步到本地注册中心
            localServiceRegister.register(remoteServices.get(0));
            return remoteServices;
        }

        // 3. 本地和远程都没有找到
        log.error("本地和远程注册中心皆未找到可用服务实例 [{}]", serviceKey);
        throw new NoAvailInstanceException("无可用实例");
    }

    /**
     * 从所有可用服务中获取具体执行方法的服务
     * @param availableServices 所有可用服务
     */
    private static ServiceMetaData getService(String serviceName, String serviceVersion, List<ServiceMetaData> availableServices,
            String loadBalancerType) {
        // 负载均衡
        LoadBalancer loadBalancer;
        try {
            Class<?> clazz = Class.forName(loadBalancerType);
            Constructor<?> constructor = clazz.getDeclaredConstructor(String.class, String.class);
            loadBalancer = (LoadBalancer) constructor.newInstance(serviceName, serviceVersion);
        } catch (Exception e) {
            log.warn("未找到负载均衡器，退化到RandomLoadBalancer, loadBalancerType={}", loadBalancerType);
            loadBalancer = new RandomLoadBalancer(serviceName, serviceVersion);
        }
        return loadBalancer.selectService(availableServices);
    }
}
