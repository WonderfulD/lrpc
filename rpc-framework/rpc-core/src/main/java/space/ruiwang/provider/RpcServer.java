package space.ruiwang.provider;

/**
 * RpcServer 接口，负责网络通信以及服务注册。
 * 可以通过 SPI 机制拓展不同的网络通信实现，目前已有 TomcatServer 的实现。
 * 同时在此接口中集成本地服务注册逻辑。
 * @author wangrui <wangrui45@kuaishou.com>
 * Created on 2025-02-11
 */
public interface RpcServer {
    void start(String hostName, int port);
}
