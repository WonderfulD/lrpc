package space.ruiwang.register.sub;

import java.util.concurrent.TimeUnit;

import space.ruiwang.domain.ServiceRegisterDO;

/**
 * @author wangrui <wangrui45@kuaishou.com>
 * Created on 2025-03-10
 */
public interface IRemoteServiceRegister extends space.ruiwang.register.IServiceRegister {
    boolean renew(ServiceRegisterDO service, Long time, TimeUnit timeUnit);
}
