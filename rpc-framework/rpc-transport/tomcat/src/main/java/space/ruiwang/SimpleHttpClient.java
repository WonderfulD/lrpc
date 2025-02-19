package space.ruiwang;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import lombok.extern.slf4j.Slf4j;
import space.ruiwang.consumer.RpcConsumer;
import space.ruiwang.domain.RpcRequestDO;
import space.ruiwang.domain.RpcResponseDO;
import space.ruiwang.domain.ServiceRegisterDO;
import space.ruiwang.servicefinder.ServiceFinder;
import space.ruiwang.servicefinder.impl.ServiceFinderImpl;
import space.ruiwang.serviceregister.impl.LocalServiceRegister;
import space.ruiwang.serviceregister.impl.RemoteServiceRegister;
import space.ruiwang.utils.InputStreamUtils;
import space.ruiwang.utils.KryoSerializer;

/**
 * @author wangrui <wangrui45@kuaishou.com>
 * Created on 2025-02-12
 */
@Slf4j
public class SimpleHttpClient implements RpcConsumer {
    @Override
    public RpcResponseDO send(RpcRequestDO rpcRequestDO, String loadBalancerType, long retryCount, long timeout, String tolerant) {
        String hostName = null;
        int port = 0;
        try {
            // 获取具体服务
            ServiceFinder serviceFinder = new ServiceFinderImpl(new LocalServiceRegister(), new RemoteServiceRegister());
            ServiceRegisterDO selectedService =
                    serviceFinder.selectService(rpcRequestDO.getServiceName(), rpcRequestDO.getServiceVersion(), loadBalancerType);
            hostName = selectedService.getServiceAddr();
            port = selectedService.getPort();
        } catch (Exception e) {
            // TODO 结合重试机制抛出有message的Exception
            log.error("rpc请求失败，错误信息:[{}]", e.getMessage());
            return RpcResponseDO.error("Rpc调用失败，无法找到实例");
        }

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
