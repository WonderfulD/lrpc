package space.ruiwang.serviceremoval;

import lombok.extern.slf4j.Slf4j;
import space.ruiwang.utils.ServiceLoaderUtil;
import space.ruiwang.api.job.IServiceRemovalJob;
import space.ruiwang.api.serviceregister.sub.ILocalServiceRegister;
import space.ruiwang.api.serviceregister.sub.IRemoteServiceRegister;
import space.ruiwang.domain.ServiceMetaData;


/**
 * @author wangrui <wangrui45@kuaishou.com>
 * Created on 2025-03-05
 */
@Deprecated
@Slf4j
public class ServiceExpiredRemoveJob implements IServiceRemovalJob {
    private ServiceExpiredRemoveUtil serviceExpiredRemoveUtil;
    private ServiceMetaData serviceMetaData;
    private ILocalServiceRegister localServiceRegister;
    private IRemoteServiceRegister remoteServiceRegister;
    private ServiceLoaderUtil serviceLoaderUtil;

    public ServiceExpiredRemoveJob(ServiceExpiredRemoveUtil serviceExpiredRemoveUtil,
            ServiceMetaData serviceMetaData,
            ILocalServiceRegister localServiceRegister, IRemoteServiceRegister remoteServiceRegister,
            ServiceLoaderUtil serviceLoaderUtil) {
        this.serviceExpiredRemoveUtil = serviceExpiredRemoveUtil;
        this.serviceMetaData = serviceMetaData;
        this.localServiceRegister = localServiceRegister;
        this.remoteServiceRegister = remoteServiceRegister;
        this.serviceLoaderUtil = serviceLoaderUtil;
    }

    @Override
    public void run() {
        // TODO 远程注册中心所有过期服务实例下线逻辑
    }
}
