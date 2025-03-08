package space.ruiwang.servicemanager.renewal;

import java.util.concurrent.TimeUnit;

import lombok.extern.slf4j.Slf4j;
import space.ruiwang.domain.ServiceRegisterDO;

/**
 * 服务续约任务
 * 模仿Redisson看门狗续约
 * 每 1/2 Unit 初始生存时间续约 1 Unit
 * @author wangrui <wangrui45@kuaishou.com>
 * Created on 2025-03-04
 */
@Slf4j
public class ServiceRenewalJob implements Runnable {
    private final ServiceRenewalUtil serviceRenewalUtil;
    private final ServiceRegisterDO service;
    private final Long time;
    private final TimeUnit timeUnit;

    public ServiceRenewalJob(ServiceRenewalUtil serviceRenewalUtil, ServiceRegisterDO service, Long time,
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
