package space.ruiwang;

import java.util.ServiceLoader;

import lombok.extern.slf4j.Slf4j;
import space.ruiwang.domain.ServiceRegisterDO;
import space.ruiwang.provider.RpcServer;
import space.ruiwang.serviceregister.ServiceRegister;
import space.ruiwang.serviceregister.impl.LocalServiceRegister;
import space.ruiwang.serviceregister.impl.RemoteServiceRegister;

/**
 * @author wangrui <wangrui45@kuaishou.com>
 * Created on 2025-02-12
 */
@Slf4j
public class ProviderTest {
    private static final String HOST_NAME = "localhost";
    private static final int PORT = 9001;
    public static void main(String[] args) {
        // 本地注册器
        LocalServiceRegister localServiceRegister = new LocalServiceRegister();
        // 远程注册器
        RemoteServiceRegister remoteServiceRegister = new RemoteServiceRegister();

        // 构建服务数据对象，不传入ttl
        ServiceRegisterDO service = new ServiceRegisterDO("space.ruiwang.service.TestService", "1.0", HOST_NAME, PORT);

        // 注册服务到本地
        localServiceRegister.register(service);
        // 注册服务到远程
        remoteServiceRegister.register(service);

        try {
            ServiceRegister.serviceImplRegister();
        } catch (Exception e) {
            log.warn(e.getMessage());
        }

        // 使用SPI机制加载RpcServer实现
        ServiceLoader<RpcServer> loader = ServiceLoader.load(RpcServer.class);
        RpcServer server = loader.findFirst().orElseThrow(() -> new RuntimeException("No RpcServer implementation found"));

        server.start(HOST_NAME, PORT);
    }
}
