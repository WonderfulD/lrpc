package space.ruiwang.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import space.ruiwang.domain.ServiceMetaData;

/**
 * @author wangrui <wangrui45@kuaishou.com>
 * Created on 2025-03-05
 */
@Component
public class ServiceStatus {

    /**
     * 单个服务是否过期，过期返回是
     */
    public boolean ifExpired(ServiceMetaData service) {
        long now = System.currentTimeMillis();
        Long endTime = service.getEndTime();
        return now > endTime;
    }

    /**
     * 获取未过期服务列表
     */
    public List<ServiceMetaData> filterUnExpired(List<ServiceMetaData> serviceList) {
        return serviceList.stream().filter(this::ifNotExpired).collect(Collectors.toList());
    }

    /**
     * 获取过期服务列表
     */
    public List<ServiceMetaData> filterExpired(List<ServiceMetaData> serviceList) {
        return serviceList.stream().filter(this::ifExpired).collect(Collectors.toList());
    }

    /**
     * 单个服务是否过期，未过期返回是
     */
    public boolean ifNotExpired(ServiceMetaData service) {
        return !ifExpired(service);
    }
}
