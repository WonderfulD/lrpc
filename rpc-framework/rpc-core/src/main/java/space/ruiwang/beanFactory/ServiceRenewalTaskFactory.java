package space.ruiwang.beanFactory;

import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;

import org.springframework.stereotype.Component;

import space.ruiwang.domain.ServiceRegisterDO;
import space.ruiwang.servicemanager.ServiceRenewalTask;
import space.ruiwang.servicemanager.ServiceRenewalUtil;

/**
 * @author wangrui <wangrui45@kuaishou.com>
 * Created on 2025-03-04
 */
@Component
public class ServiceRenewalTaskFactory {

    @Resource
    private ServiceRenewalUtil serviceRenewalUtil;

    public ServiceRenewalTask createTask(ServiceRegisterDO service, Long time, TimeUnit timeUnit) {
        // 通过带参构造注入 serviceRenewalUtil
        return new ServiceRenewalTask(serviceRenewalUtil, service, time, timeUnit);
    }
}
