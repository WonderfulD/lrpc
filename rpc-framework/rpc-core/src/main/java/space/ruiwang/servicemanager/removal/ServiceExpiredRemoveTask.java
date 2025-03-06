package space.ruiwang.servicemanager.removal;

import java.util.List;

import lombok.extern.slf4j.Slf4j;
import space.ruiwang.domain.ServiceRegisterDO;
import space.ruiwang.serviceregister.ServiceRegister;
import space.ruiwang.utils.RpcServiceKeyBuilder;

/**
 * @author wangrui <wangrui45@kuaishou.com>
 * Created on 2025-03-05
 */
@Slf4j
public class ServiceExpiredRemoveTask implements Runnable {
    private ServiceExpiredRemoveUtil serviceExpiredRemoveUtil;

    private ServiceRegisterDO serviceRegisterDO;
    private ServiceRegister localServiceRegister;
    private ServiceRegister remoteServiceRegister;

    public ServiceExpiredRemoveTask(ServiceExpiredRemoveUtil serviceExpiredRemoveUtil,
            ServiceRegisterDO serviceRegisterDO,
            ServiceRegister localServiceRegister, ServiceRegister remoteServiceRegister) {
        this.serviceExpiredRemoveUtil = serviceExpiredRemoveUtil;
        this.serviceRegisterDO = serviceRegisterDO;
        this.localServiceRegister = localServiceRegister;
        this.remoteServiceRegister = remoteServiceRegister;
    }

    @Override
    public void run() {
        // 一台服务器可以提供一个应用，一个应用包含n个服务，一个应用可以部署在n个服务器上
        // 应当剔除本地&远程中所有服务（Service）的过期服务实例
        // 1.获取服务Key
        String serviceName = serviceRegisterDO.getServiceName();
        String serviceVersion = serviceRegisterDO.getServiceVersion();
        String key = RpcServiceKeyBuilder.buildServiceKey(serviceName, serviceVersion);
        // 1.1 获取本地服务注册实例
        List<ServiceRegisterDO> localServiceList = localServiceRegister.search(key);
        localServiceList.forEach(serviceExpiredRemoveUtil::removeLocalExpiredService);
        // 1.2 获取远程服务注册实例
        List<ServiceRegisterDO> remoteServiceList = remoteServiceRegister.search(key);
        remoteServiceList.forEach(serviceExpiredRemoveUtil::removeRemoteExpiredService);
    }
}
