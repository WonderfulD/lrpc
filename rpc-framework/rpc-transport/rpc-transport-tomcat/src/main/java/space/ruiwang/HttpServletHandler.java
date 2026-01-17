package space.ruiwang;

import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import lombok.extern.slf4j.Slf4j;
import space.ruiwang.domain.RpcRequestDTO;
import space.ruiwang.domain.RpcResponseDTO;
import space.ruiwang.servicemanager.ServiceMapHolder;
import space.ruiwang.utils.InputStreamUtils;
import space.ruiwang.utils.serializer.ProtobufSerializer;

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
            RpcRequestDTO rpcRequestDTO = ProtobufSerializer.deserializeRequest(rpcRequest);

            // 从rpc请求数据载体获取：服务名、方法名、参数类型列表、参数列表
            String serviceName = rpcRequestDTO.getServiceName();
            String methodName = rpcRequestDTO.getMethodName();
            Class<?>[] parameterTypes = rpcRequestDTO.getParameterTypes();
            Object[] parameters = rpcRequestDTO.getParameters();

            // 反射获取方法执行结果
            // 根据注解@RpcService来查找实现了serviceName接口的实现类对象
            Object serviceClass = ServiceMapHolder.getService(serviceName);
            Method method = serviceClass.getClass().getMethod(methodName, parameterTypes);
            Object result = method.invoke(serviceClass, parameters);

            // 封装为RpcResponseDTO
            RpcResponseDTO rpcResponseDTO = RpcResponseDTO.success(rpcRequestDTO.getUuid(), result);

            // 序列化结果
            byte[] rpcResponse = ProtobufSerializer.serializeResponse(rpcResponseDTO);

            OutputStream outputStream = resp.getOutputStream();
            // 写入结果
            outputStream.write(rpcResponse);
        } catch (Exception e) {
            log.error("执行rpc方法出现异常: {}", e.getMessage());
        }
    }

}
