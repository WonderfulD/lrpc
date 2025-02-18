package space.ruiwang.proxy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ServiceLoader;

import lombok.extern.slf4j.Slf4j;
import space.ruiwang.consumer.RpcConsumer;
import space.ruiwang.domain.RpcRequestDO;
import space.ruiwang.domain.RpcResponseDO;

/**
 * @author wangrui <wangrui45@kuaishou.com>
 * Created on 2025-02-12
 */
@Slf4j
public class ProxyFactory {

    // 通过 SPI 机制加载 RpcConsumer 的具体实现（例如 tomcat 模块中的 SimpleHttpClient）
    private static final RpcConsumer RPC_CONSUMER = loadRpcConsumer();

    private static RpcConsumer loadRpcConsumer() {
        ServiceLoader<RpcConsumer> loader = ServiceLoader.load(RpcConsumer.class);
        return loader.findFirst()
                .orElseThrow(() -> new RuntimeException("No RpcConsumer implementation found!"));
    }

    public static <T> T getProxy(Class<T> interfaceClass) {
        Object proxyInstance = Proxy.newProxyInstance(
                ProxyFactory.class.getClassLoader(),
                new Class<?>[]{interfaceClass},
                new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) {
                        RpcRequestDO rpcRequestDO = new RpcRequestDO(
                                interfaceClass.getName(),
                                "1.0",
                                method.getName(),
                                method.getParameterTypes(),
                                args);
                        // 发送Rpc请求
                        RpcResponseDO rpcResponseDO = RPC_CONSUMER.send(rpcRequestDO);
                        String responseMsg = rpcResponseDO.getMsg();
                        log.info("rpc请求返回消息: {}", responseMsg);
                        Class<?> returnType = method.getReturnType();
                        if (returnType.equals(Void.TYPE)) {
                            return null;
                        } else {
                            return returnType.cast(rpcResponseDO.getResult());
                        }
                    }
                });
        return interfaceClass.cast(proxyInstance);
    }
}
