package space.ruiwang.servicefinder;

import java.util.List;

import space.ruiwang.domain.ServiceRegisterDO;

/**
 * @author wangrui <wangrui45@kuaishou.com>
 * Created on 2025-02-11
 */
public interface ServiceFinder {
    List<ServiceRegisterDO> getAllAvailableServices(String serviceName, String serviceVersion);

    ServiceRegisterDO selectService(String serviceName, String serviceVersion, String loadBalancerType);
}
