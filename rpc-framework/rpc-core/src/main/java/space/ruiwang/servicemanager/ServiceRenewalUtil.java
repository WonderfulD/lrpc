package space.ruiwang.servicemanager;

import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;

import org.springframework.stereotype.Component;

import lombok.SneakyThrows;
import space.ruiwang.domain.ServiceRegisterDO;
import space.ruiwang.serviceregister.ServiceRegister;

/**
 * 服务续约器
 * @author wangrui <wangrui45@kuaishou.com>
 * Created on 2025-02-21
 */
@Component
public class ServiceRenewalUtil {
    @Resource(name = "localServiceRegister")
    private ServiceRegister localServiceRegister;

    @Resource(name = "remoteServiceRegister")
    private ServiceRegister remoteServiceRegister;
    @SneakyThrows
    public boolean renewLocalAndRemoteByTime(ServiceRegisterDO service, Long time, TimeUnit timeUnit) {
        boolean localRenewed = renewLocalByTime(service, time, timeUnit);
        boolean remoteRenewed = renewRemoteByTime(service, time, timeUnit);
        return localRenewed && remoteRenewed;
    }

    @SneakyThrows
    public boolean renewLocalByTime(ServiceRegisterDO service, Long time, TimeUnit timeUnit) {
        return localServiceRegister.renew(service, time, timeUnit);
    }

    @SneakyThrows
    public boolean renewRemoteByTime(ServiceRegisterDO service, Long time, TimeUnit timeUnit) {
        return remoteServiceRegister.renew(service, time, timeUnit);
    }

}
