package space.ruiwang.servicemanager.removal;

import lombok.extern.slf4j.Slf4j;
import space.ruiwang.domain.ServiceRegisterDO;
import space.ruiwang.register.IServiceRegister;
import space.ruiwang.utils.ServiceLoaderUtil;

/**
 * @author wangrui <wangrui45@kuaishou.com>
 * Created on 2025-03-05
 */
@Slf4j
public class ServiceExpiredRemoveJob implements Runnable {
    private ServiceExpiredRemoveUtil serviceExpiredRemoveUtil;
    private ServiceRegisterDO serviceRegisterDO;
    private IServiceRegister localServiceRegister;
    private IServiceRegister remoteServiceRegister;
    private ServiceLoaderUtil serviceLoaderUtil;

    public ServiceExpiredRemoveJob(ServiceExpiredRemoveUtil serviceExpiredRemoveUtil,
            ServiceRegisterDO serviceRegisterDO,
            IServiceRegister localServiceRegister, IServiceRegister remoteServiceRegister,
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
    }
}
