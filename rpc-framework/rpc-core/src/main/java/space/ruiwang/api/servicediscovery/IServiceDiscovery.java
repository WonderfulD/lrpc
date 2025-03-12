package space.ruiwang.api.servicediscovery;

import java.util.List;

import space.ruiwang.domain.ServiceMetaData;

/**
 * @author wangrui <wangrui45@kuaishou.com>
 * Created on 2025-02-11
 */
public interface IServiceDiscovery {
    List<ServiceMetaData> getAllAvailableServices(String serviceName, String serviceVersion);

    ServiceMetaData selectService(String serviceName, String serviceVersion, String loadBalancerType);
    ServiceMetaData selectOtherService(String serviceName, String serviceVersion,
            String loadBalancerType, List<ServiceMetaData> excludedServices);
}
