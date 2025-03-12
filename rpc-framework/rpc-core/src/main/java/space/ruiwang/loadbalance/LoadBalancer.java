package space.ruiwang.loadbalance;

import java.util.List;

import space.ruiwang.domain.ServiceMetaData;

/**
 * @author wangrui <wangrui45@kuaishou.com>
 * Created on 2025-02-11
 */
public interface LoadBalancer {
    ServiceMetaData selectService(List<ServiceMetaData> availableServices);
}
