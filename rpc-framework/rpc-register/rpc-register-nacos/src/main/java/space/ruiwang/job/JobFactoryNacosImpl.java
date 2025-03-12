package space.ruiwang.job;

import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import space.ruiwang.api.job.IServiceRemovalJob;
import space.ruiwang.api.job.IServiceRenewalJob;
import space.ruiwang.api.jobfactory.IJobFactory;
import space.ruiwang.api.serviceregister.sub.ILocalServiceRegister;
import space.ruiwang.api.serviceregister.sub.IRemoteServiceRegister;
import space.ruiwang.domain.ServiceMetaData;


/**
 * @author wangrui <wangrui45@kuaishou.com>
 * Created on 2025-03-12
 */
@Component
public class JobFactoryNacosImpl implements IJobFactory {
    @Autowired
    private ILocalServiceRegister localServiceRegister;
    @Autowired
    private IRemoteServiceRegister remoteServiceRegister;

    public IServiceRenewalJob createServiceRenewalJob(ServiceMetaData service, Long time, TimeUnit timeUnit) {
        return new IServiceRenewalJob() {
            @Override
            public void run() { }
        };
    }

    public IServiceRemovalJob createServiceExpiredRemoveJob(ServiceMetaData service) {
        return new IServiceRemovalJob() {
            @Override
            public void run() { }
        };
    }
}
