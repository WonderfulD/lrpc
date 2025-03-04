package space.ruiwang.servicemanager;

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
public class ServiceRenewalTask implements Runnable {
    private ServiceRenewalUtil serviceRenewalUtil;

    private ServiceRegisterDO service;
    private Long time;
    private TimeUnit timeUnit;

    public ServiceRenewalTask(ServiceRenewalUtil serviceRenewalUtil, ServiceRegisterDO service, Long time,
            TimeUnit timeUnit) {
        this.serviceRenewalUtil = serviceRenewalUtil;
        this.service = service;
        this.time = time;
        this.timeUnit = timeUnit;
    }

    @Override
    public void run() {
        long halfTimeInMillis = timeUnit.toMillis(time) / 2;

        while (true) {
            try {
                // 执行续约
                serviceRenewalUtil.renewLocalAndRemoteByTime(service, time, timeUnit);
                log.info("续约成功 服务: [{}]", service);

                // 暂停线程，等待下一次续约
                Thread.sleep(halfTimeInMillis);
            } catch (InterruptedException e) {
                log.error("续约任务被中断 服务: [{}]", service, e);
                Thread.currentThread().interrupt(); // 重新设置中断状态
                break; // 退出循环
            } catch (Exception e) {
                log.error("续约失败 服务: [{}]", service, e);
            }
        }
    }
}
