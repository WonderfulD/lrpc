package space.ruiwang.servicemanager;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import space.ruiwang.domain.ServiceMetaData;

/**
 * 服务实例映射管理器
 * 存储服务接口名与对应的服务实例映射关系
 *
 * @author wangrui <wangrui45@kuaishou.com>
 * Created on 2025-03-10
 */
public class ServiceMapHolder {
    // 使用线程安全的 Map 存储服务接口名与对应的服务实例
    private static final Map<String, Object> SERVICE_MAP = new ConcurrentHashMap<>();

    /**
     * 注册服务实例
     *
     * @param serviceName     服务接口名
     * @param serviceInstance 服务实例
     */
    public static void putService(String serviceName, Object serviceInstance) {
        SERVICE_MAP.put(serviceName, serviceInstance);
    }

    /**
     * 获取服务实例
     *
     * @param serviceName 服务接口名
     * @return 服务实例，不存在则返回null
     */
    public static Object getService(String serviceName) {
        return SERVICE_MAP.get(serviceName);
    }

    /**
     * 该服务实例是否注册过，注册过返回true
     *
     * @param serviceList 已注册的服务列表
     * @param service     待检查的服务实例
     * @return 如果已注册返回true，否则返回false
     */
    public static boolean ifRegistered(List<ServiceMetaData> serviceList, ServiceMetaData service) {
        if (serviceList.isEmpty()) {
            return false;
        }
        return serviceList.stream().anyMatch(s -> s.getServiceAddr().equals(service.getServiceAddr())
                && s.getPort().equals(service.getPort())
                && s.getServiceName().equals(service.getServiceName())
                && s.getServiceVersion().equals(service.getServiceVersion()));
    }
}
