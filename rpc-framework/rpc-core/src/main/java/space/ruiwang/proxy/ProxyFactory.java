package space.ruiwang.proxy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import javax.annotation.Resource;

import org.springframework.stereotype.Component;

import cn.hutool.core.bean.BeanUtil;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import space.ruiwang.constants.RpcResponseCode;
import space.ruiwang.consumer.RpcConsumer;
import space.ruiwang.domain.RpcRequest;
import space.ruiwang.domain.RpcRequestConfig;
import space.ruiwang.domain.RpcRequestDO;
import space.ruiwang.domain.RpcResponseDO;
import space.ruiwang.domain.ServiceInstance;
import space.ruiwang.serviceregister.impl.LocalServiceRegister;
import space.ruiwang.serviceselector.ServiceSelector;

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
                        // 查找服务实例
                        ServiceInstance serviceInstance = null;
                        try {
                            serviceInstance = selectService(rpcRequestDO, rpcRequestConfig);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                        // 发送Rpc请求
                        RpcResponseDO rpcResponseDO = rpcConsumer.send(serviceInstance, rpcRequestDO, rpcRequestConfig);
                        Object result = parseRpcResponse(rpcResponseDO, rpcRequestDO);
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

    /**
     * 解析RpcResponse结果
     * @param rpcResponseDO
     * @return
     */
    private Object parseRpcResponse(RpcResponseDO rpcResponseDO, RpcRequestDO rpcRequestDO) {
        if (rpcResponseDO.getCode() != RpcResponseCode.SUCCESS) {
            log.error("rpc调用失败，请求参数: [{}]", rpcRequestDO);
            throw new RuntimeException(rpcResponseDO.getMsg());
        } else {
            log.info("rpc调用成功，请求参数: [{}]", rpcRequestDO);
            return rpcResponseDO.getResult();
        }
    }

    /**
     * 查找具体服务实例，带有重试机制
     * 查找出错/为空后，拉取远程注册中心所有服务实例，选择endTime最大的
     */
    @SneakyThrows
    private ServiceInstance selectService(RpcRequestDO rpcRequestDO, RpcRequestConfig rpcRequestConfig) {
        ServiceInstance serviceInstance = null;
        try {
            // 查找服务实例
            serviceInstance = serviceSelector.selectService(new RpcRequest(rpcRequestDO, rpcRequestConfig));
        } catch (Exception e) {
            log.warn("查找服务实例失败，错误原因：{}", e.getMessage());
            return retry(rpcRequestDO, rpcRequestConfig);
        }
        if (BeanUtil.isEmpty(serviceInstance)) {
            // 查找结果为空
            log.warn("无可用服务实例");
            return retry(rpcRequestDO, rpcRequestConfig);
        }
        return serviceInstance;
    }

    /**
     * 查找出错/为空后，拉取远程注册中心所有服务实例，选择endTime最大的
     */
    @SneakyThrows
    private ServiceInstance retry(RpcRequestDO rpcRequestDO, RpcRequestConfig rpcRequestConfig) {
        // 查找结果为空，进行重试
        // 1.1 拉取远程所有对应实例
        log.info("开始重新尝试查找服务实例");
        String serviceName = rpcRequestDO.getServiceName();
        String serviceVersion = rpcRequestDO.getServiceVersion();
        boolean loaded = localServiceRegister.loadService(serviceName, serviceVersion);
        ServiceInstance serviceInstance = null;
        if (loaded) {
            serviceInstance =
                    serviceSelector.selectService(new RpcRequest(rpcRequestDO, rpcRequestConfig));
        }
        if (BeanUtil.isNotEmpty(serviceInstance)) {
            return serviceInstance;
        } else {
            throw new RuntimeException("没有可用的服务实例");
        }
    }
}
