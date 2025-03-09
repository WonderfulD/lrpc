package space.ruiwang.servicemanager.removal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import space.ruiwang.domain.ServiceRegisterDO;
import space.ruiwang.register.impl.ILocalServiceRegister;
import space.ruiwang.register.impl.IRemoteServiceRegister;

/**
 * @author wangrui <wangrui45@kuaishou.com>
 * Created on 2025-03-05
 */
@Slf4j
@Component
public class ServiceExpiredRemoveUtil {
    @Autowired
    private ILocalServiceRegister localServiceRegister;

    @Autowired
    private IRemoteServiceRegister remoteServiceRegister;

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
