package space.ruiwang.servicerenewal;

import java.util.concurrent.TimeUnit;

import lombok.extern.slf4j.Slf4j;
import space.ruiwang.api.job.IServiceRenewalJob;
import space.ruiwang.domain.ServiceMetaData;

/**
 * 服务续约任务
 * 模仿Redisson看门狗续约
 * 每 1/2 Unit 初始生存时间续约 1 Unit
 * @author wangrui <wangrui45@kuaishou.com>
 * Created on 2025-03-04
 */
@Slf4j
public class ServiceRenewalJob implements IServiceRenewalJob {
    private ServiceRenewalUtil serviceRenewalUtil;
    private ServiceMetaData service;
    private Long time;
    private TimeUnit timeUnit;

    public ServiceRenewalJob(ServiceRenewalUtil serviceRenewalUtil, ServiceMetaData service, Long time,
            TimeUnit timeUnit) {
        this.serviceRenewalUtil = serviceRenewalUtil;
        this.service = service;
        this.time = time;
        this.timeUnit = timeUnit;
    }

    @Override
    public void run() {
        try {
            serviceRenewalUtil.renew(service, time, timeUnit);
        } catch (Exception e) {
            log.error("执行服务续期任务时遇到错误", e);
        }
    }
}
