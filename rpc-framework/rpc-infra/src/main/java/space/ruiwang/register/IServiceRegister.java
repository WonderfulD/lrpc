package space.ruiwang.register;

import java.util.List;

import space.ruiwang.domain.ServiceRegisterDO;

/**
 * @author wangrui <wangrui45@kuaishou.com>
 * Created on 2025-03-10
 */
public interface IServiceRegister {

    boolean register(ServiceRegisterDO service);

    boolean deregister(ServiceRegisterDO service);

    List<ServiceRegisterDO> search(String serviceKey);
}

