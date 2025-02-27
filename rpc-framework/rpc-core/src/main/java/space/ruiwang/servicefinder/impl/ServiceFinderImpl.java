package space.ruiwang.servicefinder.impl;

import java.lang.reflect.Constructor;
import java.util.List;

import lombok.extern.slf4j.Slf4j;
import space.ruiwang.domain.ServiceRegisterDO;
import space.ruiwang.loadbalance.LoadBalancer;
import space.ruiwang.loadbalance.impl.RandomLoadBalancer;
import space.ruiwang.servicefinder.ServiceFinder;
import space.ruiwang.serviceregister.impl.LocalServiceRegister;
import space.ruiwang.serviceregister.impl.RemoteServiceRegister;
import space.ruiwang.utils.RpcServiceKeyBuilder;

/**
 * @author wangrui <wangrui45@kuaishou.com>
 * Created on 2025-02-11
 */
@Slf4j
public class ServiceFinderImpl implements ServiceFinder {

    private final LocalServiceRegister localServiceRegister;

    private final RemoteServiceRegister remoteServiceRegister;

    public ServiceFinderImpl(LocalServiceRegister localServiceRegister, RemoteServiceRegister remoteServiceRegister) {
        this.localServiceRegister = localServiceRegister;
        this.remoteServiceRegister = remoteServiceRegister;
    }

    /**
     * 根据服务名和服务版本号查找具体执行方法的服务
     * @param serviceName 服务名
     * @param serviceVersion 服务版本号
     * @return 具体执行方法的服务，找不到则返回null
     */
    @Override
    public ServiceRegisterDO selectService(String serviceName, String serviceVersion, String loadBalancerType) {
        List<ServiceRegisterDO> allAvailableServices = getAllAvailableServices(serviceName, serviceVersion);
        return selectService(serviceName, serviceVersion, allAvailableServices, loadBalancerType);
    }

    /**
     * 从所有可用服务中获取具体执行方法的服务
     * @param availableServices 所有可用服务
     * @return
     */
    private static ServiceRegisterDO selectService(String serviceName, String serviceVersion, List<ServiceRegisterDO> availableServices,
            String loadBalancerType) {
        // 负载均衡
        LoadBalancer loadBalancer;
        try {
            Class<?> clazz = Class.forName(loadBalancerType);
            Constructor<?> constructor = clazz.getDeclaredConstructor(String.class, String.class);
            loadBalancer = (LoadBalancer) constructor.newInstance(serviceName, serviceVersion);
        } catch (Exception e) {
            log.error("未找到负载均衡器，退化到RandomLoadBalancer, loadBalancerType={}", loadBalancerType);
            loadBalancer = new RandomLoadBalancer(serviceName, serviceVersion);
        }
        return loadBalancer.selectService(availableServices);
    }

    /**
     * 查找服务：先查找本地注册中心，如果找不到再查找远程注册中心
     *
     * @param serviceName 服务名
     * @param serviceVersion 服务版本号
     * @return 服务对应的 Class 列表，找不到则返回null
     */
    @Override
    public List<ServiceRegisterDO> getAllAvailableServices(String serviceName, String serviceVersion) {
        // 1. 先从本地注册中心查找
        String serviceKey = RpcServiceKeyBuilder.buildServiceKey(serviceName, serviceVersion);
        List<ServiceRegisterDO> registeredServices = localServiceRegister.search(serviceKey);
        if (registeredServices != null && !registeredServices.isEmpty()) {
            log.info("本地注册中心找到服务 [{}], 返回 已登记服务 列表: {}", serviceKey, registeredServices);
            return registeredServices;
        }

        // 2. 本地没有，去远程注册中心查找
        log.warn("本地注册中心未找到服务 [{}], 正在尝试从远程注册中心查找...", serviceKey);
        List<ServiceRegisterDO> remoteServices = remoteServiceRegister.search(serviceKey);
        if (remoteServices != null && !remoteServices.isEmpty()) {
            log.info("远程注册中心找到服务 [{}], 返回 远程服务中心登记的服务 列表: {}", serviceKey, remoteServices);
            // 可选择将远程服务同步到本地注册中心
            remoteServices.forEach(localServiceRegister::register);
            return remoteServices;
        }

        // 3. 本地和远程都没有找到
        log.error("本地和远程注册中心皆未找到服务 [{}]", serviceKey);
        return null;
    }

}
