package space.ruiwang.api.transport;

/**
 * RpcProvider 接口，负责网络通信以及服务注册
 * 可以通过 SPI 机制拓展不同的网络通信实现
 * @author wangrui <wangrui45@kuaishou.com>
 * Created on 2025-02-11
 */
public interface RpcProvider {
    void start(String hostName, int port);
}
