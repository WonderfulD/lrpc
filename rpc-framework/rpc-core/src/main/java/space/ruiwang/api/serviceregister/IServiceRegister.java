package space.ruiwang.api.serviceregister;

import java.util.List;

import space.ruiwang.domain.ServiceMetaData;

/**
 * @author wangrui <wangrui45@kuaishou.com>
 * Created on 2025-03-10
 */
public interface IServiceRegister {

    boolean register(ServiceMetaData service);

    boolean deregister(ServiceMetaData service);

    List<ServiceMetaData> search(String serviceKey);
}

