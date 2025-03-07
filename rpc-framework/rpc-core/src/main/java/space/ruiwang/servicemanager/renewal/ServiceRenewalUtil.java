package space.ruiwang.servicemanager.renewal;

import static space.ruiwang.domain.ServiceRegisterDO.generateNewService;

import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import space.ruiwang.domain.ServiceRegisterDO;
import space.ruiwang.serviceregister.impl.LocalServiceRegister;
import space.ruiwang.serviceregister.impl.RemoteServiceRegister;
import space.ruiwang.utils.RpcServiceKeyBuilder;

/**
 * 服务续约器
 * 只更新远程注册中心
 * 本地注册中心的任何修改只是从远程拉取
 * @author wangrui <wangrui45@kuaishou.com>
 * Created on 2025-02-21
 */
@Slf4j
@Component
public class ServiceRenewalUtil {
    @Resource
    private RemoteServiceRegister remoteServiceRegister;

    @Resource
    private LocalServiceRegister localServiceRegister;

    public boolean renew(ServiceRegisterDO service, Long time, TimeUnit timeUnit) {
        boolean renewed = remoteServiceRegister.renew(service, time, timeUnit);
        String serviceKey = RpcServiceKeyBuilder.buildServiceKey(service.getServiceName(), service.getServiceVersion());
        if (renewed) {
            log.warn("续约成功，开始从远程服务注册中心拉取该服务的所有实例镜像列表，服务:[{}]", serviceKey);
            localServiceRegister.loadService(service.getServiceName(), service.getServiceVersion());
            return true;
        }
        try {
            // 续约失败，重新注册本服务实例
            long now = System.currentTimeMillis();
            long renewedTime = timeUnit.toMillis(time);
            service.setEndTime(now + renewedTime);
            log.warn("续约失败，重新注册该服务实例，服务:[{}]", service);
            remoteServiceRegister.register(generateNewService(service));
            log.info("服务重新注册成功，开始从远程服务注册中心拉取所属服务的所有实例镜像列表，服务:[{}]", serviceKey);
            localServiceRegister.loadService(service.getServiceName(), service.getServiceVersion());
            return true;
        } catch (Exception e) {
            log.error("续约&服务实例重新注册失败，服务:[{}]", service);
            return false;
        }
    }

}
