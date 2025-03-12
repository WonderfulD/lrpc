package space.ruiwang.serviceremoval;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import space.ruiwang.api.serviceregister.sub.ILocalServiceRegister;
import space.ruiwang.api.serviceregister.sub.IRemoteServiceRegister;
import space.ruiwang.domain.ServiceMetaData;


/**
 * @author wangrui <wangrui45@kuaishou.com>
 * Created on 2025-03-05
 */
@Deprecated
@Slf4j
@Component
public class ServiceExpiredRemoveUtil {
    @Autowired
    private ILocalServiceRegister localServiceRegister;

    @Autowired
    private IRemoteServiceRegister remoteServiceRegister;

    public boolean removeLocalExpiredService(ServiceMetaData service) {
        if (service == null) {
            return false;
        }
        return localServiceRegister.deregister(service);
    }

    public boolean removeRemoteExpiredService(ServiceMetaData service) {
        if (service == null) {
            return false;
        }
        return remoteServiceRegister.deregister(service);
    }
}
