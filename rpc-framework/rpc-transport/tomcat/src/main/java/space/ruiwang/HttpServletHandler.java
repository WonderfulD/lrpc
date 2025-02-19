package space.ruiwang;

import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;


import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import lombok.extern.slf4j.Slf4j;
import space.ruiwang.domain.RpcRequestDO;
import space.ruiwang.domain.RpcResponseDO;
import space.ruiwang.serviceregister.ServiceRegister;
import space.ruiwang.utils.InputStreamUtils;
import space.ruiwang.utils.KryoSerializer;

/**
 * @author wangrui <wangrui45@kuaishou.com>
 * Created on 2025-02-11
 */
@Slf4j
public class HttpServletHandler {
    public void handle(HttpServletRequest req, HttpServletResponse resp) {
        try {
            // 读取序列化后的请求
            InputStream inputStream = req.getInputStream();
            byte[] rpcRequest = InputStreamUtils.readAllBytes(inputStream);

            // 反序列化请求
            RpcRequestDO rpcRequestDO = KryoSerializer.deserialize(rpcRequest, RpcRequestDO.class);

            // 从rpc请求数据载体获取：服务名、方法名、参数类型列表、参数列表
            String serviceName = rpcRequestDO.getServiceName();
            String methodName = rpcRequestDO.getMethodName();
            Class<?>[] parameterTypes = rpcRequestDO.getParameterTypes();
            Object[] parameters = rpcRequestDO.getParameters();

            // 反射获取方法执行结果
            // 根据注解@RpcService来查找实现了serviceName接口的实现类对象
            Object serviceClass = ServiceRegister.getService(serviceName);
            Method method = serviceClass.getClass().getMethod(methodName, parameterTypes);
            Object result = method.invoke(serviceClass, parameters);

            // 封装为RpcResponseDO
            RpcResponseDO rpcResponseDO = RpcResponseDO.success(result);

            // 序列化结果
            byte[] rpcResponse = KryoSerializer.serialize(rpcResponseDO);

            OutputStream outputStream = resp.getOutputStream();
            // 写入结果
            outputStream.write(rpcResponse);
        } catch (Exception e) {
            log.error("执行rpc方法出现异常: {}", e.getMessage());
        }
    }

}
