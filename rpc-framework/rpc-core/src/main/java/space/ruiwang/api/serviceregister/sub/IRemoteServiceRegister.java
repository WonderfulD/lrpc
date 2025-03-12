package space.ruiwang.api.serviceregister.sub;

import java.util.concurrent.TimeUnit;

import space.ruiwang.domain.ServiceMetaData;
import space.ruiwang.api.serviceregister.IServiceRegister;

/**
 * @author wangrui <wangrui45@kuaishou.com>
 * Created on 2025-03-10
 */
public interface IRemoteServiceRegister extends IServiceRegister {
    boolean renew(ServiceMetaData service, Long time, TimeUnit timeUnit);
}
