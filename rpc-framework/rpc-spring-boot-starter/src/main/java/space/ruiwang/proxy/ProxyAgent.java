package space.ruiwang.proxy;

import static space.ruiwang.domain.RpcRequestDTO.buildRpcRequestDTO;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Resource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import space.ruiwang.api.serviceregister.sub.ILocalServiceRegister;
import space.ruiwang.api.serviceselector.IServiceSelector;
import space.ruiwang.api.transport.RpcConsumer;
import space.ruiwang.constants.RpcResponseCode;
import space.ruiwang.domain.RpcRequest;
import space.ruiwang.domain.RpcRequestConfig;
import space.ruiwang.domain.RpcRequestDTO;
import space.ruiwang.domain.RpcResponseDTO;
import space.ruiwang.domain.ServiceInstance;
import space.ruiwang.domain.ServiceMetaData;
import space.ruiwang.tolerant.FaultTolerant;


/**
 * @author wangrui <wangrui45@kuaishou.com>
 * Created on 2025-02-12
 */
@Slf4j
@Component
public class ProxyAgent {
    @Resource
    private RpcConsumer rpcConsumer;
    @Resource
    private IServiceSelector serviceSelector;
    @Autowired
    private ILocalServiceRegister localServiceRegister;

    public <T> T getProxy(Class<T> interfaceClass,
            String serviceVersion, RpcRequestConfig rpcRequestConfig) {
        Object proxyInstance = Proxy.newProxyInstance(
                ProxyAgent.class.getClassLoader(),
                new Class<?>[]{interfaceClass},
                new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) {
                        RpcRequestDTO rpcRequestDTO = buildRpcRequestDTO(interfaceClass.getName(),
                                                                         serviceVersion,
                                                                         method.getName(),
                                                                         method.getParameterTypes(),
                                                                         args);
                        Object result = handle(rpcRequestDTO, rpcRequestConfig, new ArrayList<>());
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

    private Object handle(RpcRequestDTO rpcRequestDTO, RpcRequestConfig rpcRequestConfig, List<ServiceMetaData> excludedServices) {
        while (rpcRequestConfig.getRetryCount() > 0) {
            // 查找服务实例
            ServiceMetaData service;
            try {
                service = selectService(rpcRequestDTO, rpcRequestConfig, excludedServices);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            try {
                // 发送Rpc请求
                ServiceInstance serviceInstance = new ServiceInstance(service);
                RpcResponseDTO rpcResponseDTO = rpcConsumer.send(serviceInstance, rpcRequestDTO, rpcRequestConfig);
                return checkRpcResponse(rpcResponseDTO, rpcRequestDTO);
            } catch (Exception e) {
                // rpc请求发生错误，重试
                faultTolerant(rpcRequestDTO, rpcRequestConfig, excludedServices, service);
            }
        }
        throw new RuntimeException("rpc调用失败");
    }

    /**
     * 检查RpcResponse结果
     */
    private Object checkRpcResponse(RpcResponseDTO rpcResponseDTO, RpcRequestDTO rpcRequestDTO) {
        if (rpcResponseDTO.getCode() != RpcResponseCode.SUCCESS) {
            log.error("rpc调用遇到错误，请求参数: [{}]", rpcRequestDTO);
            throw new RuntimeException("rpc调用失败");
        } else {
            log.info("rpc调用成功，请求参数: [{}]", rpcRequestDTO);
            return rpcResponseDTO.getResult();
        }
    }

    /**
     * 请求失败后重试机制
     */
    @SneakyThrows
    private void faultTolerant(RpcRequestDTO rpcRequestDTO, RpcRequestConfig rpcRequestConfig,
            List<ServiceMetaData> excludedServices, ServiceMetaData service) {
        String tolerantType = rpcRequestConfig.getTolerant();
        Class<?> clazz = Class.forName(tolerantType);
        Constructor<?> constructor = clazz.getDeclaredConstructor();
        FaultTolerant tolerant = (FaultTolerant) constructor.newInstance();
        tolerant.handler(rpcRequestDTO, rpcRequestConfig, excludedServices, service);
    }

    /**
     * 查找具体服务实例，带有重试机制
     * 查找出错/为空后，拉取远程注册中心所有服务实例，选择endTime最大的
     */
    @SneakyThrows
    private ServiceMetaData selectService(RpcRequestDTO rpcRequestDTO, RpcRequestConfig rpcRequestConfig,
            List<ServiceMetaData> excludedServices) {
        ServiceMetaData service;
        try {
            // 查找服务实例
            if (CollUtil.isEmpty(excludedServices)) {
                // 排除的服务实例列表为空
                service = serviceSelector.selectService(new RpcRequest(rpcRequestDTO, rpcRequestConfig));
            } else {
                service = serviceSelector.selectOtherService(new RpcRequest(rpcRequestDTO, rpcRequestConfig), excludedServices);
            }
        } catch (Exception e) {
            log.warn("查找服务实例失败，错误原因：{}", e.getMessage());
            return retry(rpcRequestDTO, rpcRequestConfig, excludedServices);
        }
        if (BeanUtil.isEmpty(service)) {
            // 查找结果为空
            log.warn("无可用服务实例");
            return retry(rpcRequestDTO, rpcRequestConfig, excludedServices);
        }
        return service;
    }

    /**
     * 查找出错/为空后，拉取远程注册中心所有服务实例，选择endTime最大的
     */
    @SneakyThrows
    private ServiceMetaData retry(RpcRequestDTO rpcRequestDTO, RpcRequestConfig rpcRequestConfig,
            List<ServiceMetaData> excludedServices) {
        // 查找结果为空，进行重试
        // 1.1 拉取远程所有对应实例
        log.info("开始重新尝试查找服务实例");
        String serviceName = rpcRequestDTO.getServiceName();
        String serviceVersion = rpcRequestDTO.getServiceVersion();
        boolean loaded = localServiceRegister.loadService(serviceName, serviceVersion);
        ServiceMetaData service = null;
        if (loaded) {
            if (CollUtil.isEmpty(excludedServices)) {
                // 排除的服务实例列表为空
                service = serviceSelector.selectService(new RpcRequest(rpcRequestDTO, rpcRequestConfig));
            } else {
                service = serviceSelector.selectOtherService(new RpcRequest(rpcRequestDTO, rpcRequestConfig), excludedServices);
            }
        }
        if (BeanUtil.isNotEmpty(service)) {
            return service;
        } else {
            throw new RuntimeException("没有可用的服务实例");
        }
    }
}
