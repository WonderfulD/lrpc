package space.ruiwang.servicemanager.removal;

import java.util.ArrayList;
import java.util.List;

import lombok.extern.slf4j.Slf4j;
import space.ruiwang.domain.ServiceRegisterDO;
import space.ruiwang.servicemanager.ServiceLoaderUtil;
import space.ruiwang.serviceregister.ServiceRegister;

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

    private ServiceLoaderUtil serviceLoaderUtil;

    public ServiceExpiredRemoveTask(ServiceExpiredRemoveUtil serviceExpiredRemoveUtil,
            ServiceRegisterDO serviceRegisterDO,
            ServiceRegister localServiceRegister, ServiceRegister remoteServiceRegister,
            ServiceLoaderUtil serviceLoaderUtil) {
        this.serviceExpiredRemoveUtil = serviceExpiredRemoveUtil;
        this.serviceRegisterDO = serviceRegisterDO;
        this.localServiceRegister = localServiceRegister;
        this.remoteServiceRegister = remoteServiceRegister;
        this.serviceLoaderUtil = serviceLoaderUtil;
    }

    @Override
    public void run() {
        // TODO 远程注册中心所有过期服务实例下线逻辑
        // 本地注册中心同步
        List<String> changedServiceList = new ArrayList<>();
        changedServiceList.forEach(serviceLoaderUtil::loadService);
    }
}
