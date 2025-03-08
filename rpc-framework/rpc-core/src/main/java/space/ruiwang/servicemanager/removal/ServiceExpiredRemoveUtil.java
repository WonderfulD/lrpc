package space.ruiwang.servicemanager.removal;

import javax.annotation.Resource;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import space.ruiwang.domain.ServiceRegisterDO;
import space.ruiwang.serviceregister.ServiceRegister;

/**
 * @author wangrui <wangrui45@kuaishou.com>
 * Created on 2025-03-05
 */
@Slf4j
@Component
public class ServiceExpiredRemoveUtil {
    @Resource(name = "localServiceRegister")
    private ServiceRegister localServiceRegister;

    @Resource(name = "remoteServiceRegister")
    private ServiceRegister remoteServiceRegister;

    public boolean removeLocalExpiredService(ServiceRegisterDO service) {
        if (service == null) {
            return false;
        }
        return localServiceRegister.deregister(service);
    }

    public boolean removeRemoteExpiredService(ServiceRegisterDO service) {
        if (service == null) {
            return false;
        }
        return remoteServiceRegister.deregister(service);
    }
}
