package space.ruiwang.impl;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.alibaba.nacos.api.naming.NamingService;
import com.google.gson.Gson;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import lombok.SneakyThrows;
import space.ruiwang.domain.ServiceRegisterDO;

/**
 * @author wangrui <wangrui45@kuaishou.com>
 * Created on 2025-03-10
 */
@Component
public class ServiceUtil {

    /**
     * Nacos 命名服务
     */
    @Autowired
    private NamingService namingService;

    /**
     * 将 service 对象转换为 Map<String, String>
     */
    public static Map<String, String> toMap(ServiceRegisterDO service) {
        if (BeanUtil.isEmpty(service)) {
            return Collections.emptyMap();
        }

        Map<String, String> resultMap = new HashMap<>();
        resultMap.put("uuid", service.getUuid());
        resultMap.put("serviceName", service.getServiceName());
        resultMap.put("serviceVersion", service.getServiceVersion());
        resultMap.put("serviceAddr", service.getServiceAddr());
        resultMap.put("port", service.getPort() != null ? service.getPort().toString() : null);
        resultMap.put("endTime", service.getEndTime() != null ? service.getEndTime().toString() : null);

        return resultMap;
    }

    /**
     * 将 Map<String, String> 转换为 ServiceRegisterDO 实例
     */
    public static ServiceRegisterDO toService(Map<String, String> map) {
        if (CollUtil.isEmpty(map)) {
            return null;
        }

        ServiceRegisterDO service = new ServiceRegisterDO(
                map.get("serviceName"),
                map.get("serviceVersion"),
                map.get("serviceAddr"),
                map.get("port") != null ? Integer.parseInt(map.get("port")) : null
        );

        service.setUuid(map.get("uuid"));

        if (map.get("endTime") != null) {
            service.setEndTime(Long.parseLong(map.get("endTime")));
        }

        return service;
    }

    @SneakyThrows
    public List<ServiceRegisterDO> loadServices(String serviceKey) {
        return namingService.getAllInstances(serviceKey).stream()
                .map(instance -> ServiceUtil.toService(instance.getMetadata()))
                .collect(Collectors.toList());
    }
}
