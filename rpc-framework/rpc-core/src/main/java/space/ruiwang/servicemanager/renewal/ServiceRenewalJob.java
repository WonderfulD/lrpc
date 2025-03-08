package space.ruiwang.servicemanager.renewal;

import java.util.concurrent.TimeUnit;

import lombok.SneakyThrows;
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
    private ServiceRenewalUtil serviceRenewalUtil;

    private ServiceRegisterDO service;
    private Long time;
    private TimeUnit timeUnit;

    public ServiceRenewalJob(ServiceRenewalUtil serviceRenewalUtil, ServiceRegisterDO service, Long time,
                             TimeUnit timeUnit) {
        this.serviceRenewalUtil = serviceRenewalUtil;
        this.service = service;
        this.time = time;
        this.timeUnit = timeUnit;
    }
    @Override
    @SneakyThrows
    public void run() {
        serviceRenewalUtil.renew(service, time, timeUnit);
    }
}
