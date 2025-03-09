package space.ruiwang.register.impl;

import space.ruiwang.domain.ServiceRegisterDO;
import space.ruiwang.register.IServiceRegister;

/**
 * @author wangrui <wangrui45@kuaishou.com>
 * Created on 2025-03-10
 */
public interface ILocalServiceRegister extends IServiceRegister {
    boolean deregister(ServiceRegisterDO serviceRegisterDO);
    boolean loadService(String serviceName, String serviceVersion);

}
