package space.ruiwang.api.jobfactory;

import java.util.concurrent.TimeUnit;

import space.ruiwang.api.job.IServiceRemovalJob;
import space.ruiwang.api.job.IServiceRenewalJob;
import space.ruiwang.domain.ServiceMetaData;

/**
 * @author wangrui <wangrui45@kuaishou.com>
 * Created on 2025-03-12
 */
public interface IJobFactory {
    IServiceRenewalJob createServiceRenewalJob(ServiceMetaData service, Long time, TimeUnit timeUnit);

    IServiceRemovalJob createServiceExpiredRemoveJob(ServiceMetaData service);
}
