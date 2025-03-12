package space.ruiwang.job;

import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import space.ruiwang.utils.ServiceLoaderUtil;
import space.ruiwang.api.job.IServiceRemovalJob;
import space.ruiwang.api.job.IServiceRenewalJob;
import space.ruiwang.api.jobfactory.IJobFactory;
import space.ruiwang.api.serviceregister.sub.ILocalServiceRegister;
import space.ruiwang.api.serviceregister.sub.IRemoteServiceRegister;
import space.ruiwang.domain.ServiceMetaData;
import space.ruiwang.serviceremoval.ServiceExpiredRemoveJob;
import space.ruiwang.serviceremoval.ServiceExpiredRemoveUtil;
import space.ruiwang.servicerenewal.ServiceRenewalJob;
import space.ruiwang.servicerenewal.ServiceRenewalUtil;


/**
 * @author wangrui <wangrui45@kuaishou.com>
 * Created on 2025-03-12
 */
@Component
public class JobFactoryRedisImpl implements IJobFactory {
    @Resource
    private ServiceRenewalUtil serviceRenewalUtil;
    @Resource
    private ServiceExpiredRemoveUtil serviceExpiredRemoveUtil;
    @Autowired
    private ILocalServiceRegister localServiceRegister;
    @Autowired
    private IRemoteServiceRegister remoteServiceRegister;
    @Autowired
    private ServiceLoaderUtil serviceLoaderUtil;

    public IServiceRenewalJob createServiceRenewalJob(ServiceMetaData service, Long time, TimeUnit timeUnit) {
        // 通过带参构造注入 serviceRenewalUtil
        return new ServiceRenewalJob(serviceRenewalUtil, service, time, timeUnit);
    }

    public IServiceRemovalJob createServiceExpiredRemoveJob(ServiceMetaData service) {
        return new ServiceExpiredRemoveJob(serviceExpiredRemoveUtil, service, localServiceRegister, remoteServiceRegister, serviceLoaderUtil);
    }
}
