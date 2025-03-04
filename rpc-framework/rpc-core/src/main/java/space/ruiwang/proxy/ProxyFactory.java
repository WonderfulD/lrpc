package space.ruiwang.proxy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import javax.annotation.Resource;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import space.ruiwang.constants.RpcResponseCode;
import space.ruiwang.consumer.RpcConsumer;
import space.ruiwang.domain.RpcRequestConfig;
import space.ruiwang.domain.RpcRequestDO;
import space.ruiwang.domain.RpcResponseDO;

/**
 * @author wangrui <wangrui45@kuaishou.com>
 * Created on 2025-02-12
 */
@Slf4j
@Component
public class ProxyFactory {
    @Resource
    private RpcConsumer rpcConsumer;
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
                        // 发送Rpc请求
                        RpcResponseDO rpcResponseDO = rpcConsumer.send(rpcRequestDO, rpcRequestConfig);
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
}
