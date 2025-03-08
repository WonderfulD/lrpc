package space.ruiwang.factory.beanFactory;

import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;

import org.springframework.context.annotation.Configuration;

import space.ruiwang.domain.ServiceRegisterDO;
import space.ruiwang.servicemanager.ServiceLoaderUtil;
import space.ruiwang.servicemanager.removal.ServiceExpiredRemoveJob;
import space.ruiwang.servicemanager.removal.ServiceExpiredRemoveUtil;
import space.ruiwang.servicemanager.renewal.ServiceRenewalJob;
import space.ruiwang.servicemanager.renewal.ServiceRenewalUtil;
import space.ruiwang.serviceregister.ServiceRegister;

/**
 * @author wangrui <wangrui45@kuaishou.com>
 * Created on 2025-03-04
 */
@Configuration
public class TaskFactory {

    @Resource
    private ServiceRenewalUtil serviceRenewalUtil;
    @Resource
    private ServiceExpiredRemoveUtil serviceExpiredRemoveUtil;
    @Resource(name = "localServiceRegister")
    private ServiceRegister localServiceRegister;
    @Resource(name = "remoteServiceRegister")
    private ServiceRegister remoteServiceRegister;
    @Resource
    private ServiceLoaderUtil serviceLoaderUtil;

    public ServiceRenewalJob createServiceRenewalJob(ServiceRegisterDO service, Long time, TimeUnit timeUnit) {
        // 通过带参构造注入 serviceRenewalUtil
        return new ServiceRenewalJob(serviceRenewalUtil, service, time, timeUnit);
    }

    public ServiceExpiredRemoveJob createServiceExpiredRemoveJob(ServiceRegisterDO service) {
        return new ServiceExpiredRemoveJob(serviceExpiredRemoveUtil, service, localServiceRegister, remoteServiceRegister, serviceLoaderUtil);
    }
}
