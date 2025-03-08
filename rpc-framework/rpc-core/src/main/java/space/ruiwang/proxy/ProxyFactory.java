package space.ruiwang.proxy;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Resource;

import org.springframework.stereotype.Component;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import space.ruiwang.constants.RpcResponseCode;
import space.ruiwang.consumer.RpcConsumer;
import space.ruiwang.domain.RpcRequest;
import space.ruiwang.domain.RpcRequestConfig;
import space.ruiwang.domain.RpcRequestDO;
import space.ruiwang.domain.RpcResponseDO;
import space.ruiwang.domain.ServiceInstance;
import space.ruiwang.domain.ServiceRegisterDO;
import space.ruiwang.serviceregister.impl.LocalServiceRegister;
import space.ruiwang.serviceselector.ServiceSelector;
import space.ruiwang.tolerant.Tolerant;

/**
 * @author wangrui <wangrui45@kuaishou.com>
 * Created on 2025-02-12
 */
@Slf4j
@Component
public class ProxyFactory {
    @Resource
    private RpcConsumer rpcConsumer;
    @Resource
    private ServiceSelector serviceSelector;
    @Resource
    private LocalServiceRegister localServiceRegister;

    public <T> T getProxy(Class<T> interfaceClass,
            String serviceVersion, RpcRequestConfig rpcRequestConfig) {
        Object proxyInstance = Proxy.newProxyInstance(
                ProxyFactory.class.getClassLoader(),
                new Class<?>[]{interfaceClass},
                new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) {
                        RpcRequestDO rpcRequestDO = new RpcRequestDO(
                                interfaceClass.getName(),
                                serviceVersion,
                                method.getName(),
                                method.getParameterTypes(),
                                args);
                        Object result = handle(rpcRequestDO, rpcRequestConfig, new ArrayList<>());
                        Class<?> returnType = method.getReturnType();
                        if (returnType.equals(Void.TYPE)) {
                            return null;
                        } else {
                            return returnType.cast(result);
                        }
                    }
                });
        return interfaceClass.cast(proxyInstance);
    }

    private Object handle(RpcRequestDO rpcRequestDO, RpcRequestConfig rpcRequestConfig, List<ServiceRegisterDO> excludedServices) {
        while (rpcRequestConfig.getRetryCount() > 0) {
            // 查找服务实例
            ServiceRegisterDO service;
            try {
                service = selectService(rpcRequestDO, rpcRequestConfig, excludedServices);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            try {
                // 发送Rpc请求
                String serviceAddr = service.getServiceAddr();
                Integer port = service.getPort();
                ServiceInstance serviceInstance = new ServiceInstance(serviceAddr, port);
                RpcResponseDO rpcResponseDO = rpcConsumer.send(serviceInstance, rpcRequestDO, rpcRequestConfig);
                return checkRpcResponse(rpcResponseDO, rpcRequestDO);
            } catch (Exception e) {
                // rpc请求发生错误，重试
                faultTolerant(rpcRequestDO, rpcRequestConfig, excludedServices, service);
            }
        }
        throw new RuntimeException("rpc调用失败");
    }

    /**
     * 检查RpcResponse结果
     */
    private Object checkRpcResponse(RpcResponseDO rpcResponseDO, RpcRequestDO rpcRequestDO) {
        if (rpcResponseDO.getCode() != RpcResponseCode.SUCCESS) {
            log.error("rpc调用遇到错误，请求参数: [{}]", rpcRequestDO);
            throw new RuntimeException("rpc调用失败");
        } else {
            log.info("rpc调用成功，请求参数: [{}]", rpcRequestDO);
            return rpcResponseDO.getResult();
        }
    }

    /**
     * 请求失败后重试机制
     */
    @SneakyThrows
    private void faultTolerant(RpcRequestDO rpcRequestDO, RpcRequestConfig rpcRequestConfig,
            List<ServiceRegisterDO> excludedServices, ServiceRegisterDO service) {
        String tolerantType = rpcRequestConfig.getTolerant();
        Class<?> clazz = Class.forName(tolerantType);
        Constructor<?> constructor = clazz.getDeclaredConstructor();
        Tolerant tolerant = (Tolerant) constructor.newInstance();
        tolerant.handler(rpcRequestDO, rpcRequestConfig, excludedServices, service);
    }

    /**
     * 查找具体服务实例，带有重试机制
     * 查找出错/为空后，拉取远程注册中心所有服务实例，选择endTime最大的
     */
    @SneakyThrows
    private ServiceRegisterDO selectService(RpcRequestDO rpcRequestDO, RpcRequestConfig rpcRequestConfig,
            List<ServiceRegisterDO> excludedServices) {
        ServiceRegisterDO service;
        try {
            // 查找服务实例
            if (CollUtil.isEmpty(excludedServices)) {
                // 排除的服务实例列表为空
                service = serviceSelector.selectService(new RpcRequest(rpcRequestDO, rpcRequestConfig));
            } else {
                service = serviceSelector.selectOtherService(new RpcRequest(rpcRequestDO, rpcRequestConfig), excludedServices);
            }
        } catch (Exception e) {
            log.warn("查找服务实例失败，错误原因：{}", e.getMessage());
            return retry(rpcRequestDO, rpcRequestConfig, excludedServices);
        }
        if (BeanUtil.isEmpty(service)) {
            // 查找结果为空
            log.warn("无可用服务实例");
            return retry(rpcRequestDO, rpcRequestConfig, excludedServices);
        }
        return service;
    }

    /**
     * 查找出错/为空后，拉取远程注册中心所有服务实例，选择endTime最大的
     */
    @SneakyThrows
    private ServiceRegisterDO retry(RpcRequestDO rpcRequestDO, RpcRequestConfig rpcRequestConfig,
            List<ServiceRegisterDO> excludedServices) {
        // 查找结果为空，进行重试
        // 1.1 拉取远程所有对应实例
        log.info("开始重新尝试查找服务实例");
        String serviceName = rpcRequestDO.getServiceName();
        String serviceVersion = rpcRequestDO.getServiceVersion();
        boolean loaded = localServiceRegister.loadService(serviceName, serviceVersion);
        ServiceRegisterDO service = null;
        if (loaded) {
            if (CollUtil.isEmpty(excludedServices)) {
                // 排除的服务实例列表为空
                service = serviceSelector.selectService(new RpcRequest(rpcRequestDO, rpcRequestConfig));
            } else {
                service = serviceSelector.selectOtherService(new RpcRequest(rpcRequestDO, rpcRequestConfig), excludedServices);
            }
        }
        if (BeanUtil.isNotEmpty(service)) {
            return service;
        } else {
            throw new RuntimeException("没有可用的服务实例");
        }
    }
}
