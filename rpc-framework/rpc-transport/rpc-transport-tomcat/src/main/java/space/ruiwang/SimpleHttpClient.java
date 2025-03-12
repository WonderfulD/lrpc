package space.ruiwang;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import space.ruiwang.api.transport.RpcConsumer;
import space.ruiwang.domain.RpcRequestConfig;
import space.ruiwang.domain.RpcRequestDO;
import space.ruiwang.domain.RpcResponseDO;
import space.ruiwang.domain.ServiceInstance;
import space.ruiwang.utils.InputStreamUtils;
import space.ruiwang.utils.KryoSerializer;

/**
 * @author wangrui <wangrui45@kuaishou.com>
 * Created on 2025-02-12
 */
@Component
@Slf4j
public class SimpleHttpClient implements RpcConsumer {
    @Override
    public RpcResponseDO send(ServiceInstance serviceInstance, RpcRequestDO rpcRequestDO, RpcRequestConfig rpcRequestConfig) {
        String hostName = serviceInstance.getHostname();
        int port = serviceInstance.getPort();

        try {
            URL url = new URL("http", hostName, port, "/");
            HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();

            httpURLConnection.setRequestMethod("POST");
            httpURLConnection.setDoOutput(true);

            // 获取输出流
            OutputStream outputStream = httpURLConnection.getOutputStream();

            // 序列化rpcRequestDO
            byte[] rpcRequest = KryoSerializer.serialize(rpcRequestDO);

            // 发送请求
            outputStream.write(rpcRequest);
            outputStream.flush();
            outputStream.close();

            // 读取方法返回结果
            InputStream inputStream = httpURLConnection.getInputStream();
            byte[] responseBytes = InputStreamUtils.readAllBytes(inputStream);

            // 获得结果，反序列化后并返回
            return KryoSerializer.deserialize(responseBytes, RpcResponseDO.class);
        } catch (Exception e) {
            log.error("发送rpc请求时失败");
        }
        return null;
    }
}
