package space.ruiwang.servicemanager.renewal;

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

    public boolean renewLocalByTime(ServiceRegisterDO service, Long time, TimeUnit timeUnit) {
        boolean renewed = localServiceRegister.renew(service, time, timeUnit);
        if (renewed) {
            // 续约成功
            return true;
        }
        try {
            // 续约失败，重新注册本服务实例
            long now = System.currentTimeMillis();
            long renewedTime = timeUnit.toMillis(time);
            service.setEndTime(now + renewedTime);
            localServiceRegister.register(service);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @SneakyThrows
    public boolean renewRemoteByTime(ServiceRegisterDO service, Long time, TimeUnit timeUnit) {
        boolean renewed = remoteServiceRegister.renew(service, time, timeUnit);
        if (renewed) {
            return true;
        }
        try {
            // 续约失败，重新注册本服务实例
            long now = System.currentTimeMillis();
            long renewedTime = timeUnit.toMillis(time);
            service.setEndTime(now + renewedTime);
            remoteServiceRegister.register(service);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

}
