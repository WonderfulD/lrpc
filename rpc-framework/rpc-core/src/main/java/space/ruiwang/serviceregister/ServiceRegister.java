package space.ruiwang.serviceregister;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.reflections.Reflections;

import space.ruiwang.annotation.RpcService;
import space.ruiwang.domain.ServiceRegisterDO;

/**
 * @author wangrui <wangrui45@kuaishou.com>
 * Created on 2025-02-11
 */
public interface ServiceRegister {
    // 使用线程安全的 Map 存储服务接口名与对应的服务实例
    Map<String, Object> SERVICE_MAP = new ConcurrentHashMap<>();

    boolean register(ServiceRegisterDO service);

    boolean deregister(ServiceRegisterDO service);

    List<ServiceRegisterDO> search(String serviceKey);
    boolean renew(ServiceRegisterDO service, Long time, TimeUnit timeUnit);

    /**
     * 该服务实例是否注册过，注册过返回true
     */
    default boolean ifRegistered(List<ServiceRegisterDO> serviceList, ServiceRegisterDO service) {
        if (serviceList.isEmpty()) {
            // 空List，返回false
            return false;
        }
        return serviceList.stream().anyMatch(s -> {
            return s.getServiceAddr().equals(service.getServiceAddr())
                    &&
                    s.getPort().equals(service.getPort());
        });
    }

    // 扫描指定包下所有标注了 @RpcService 的服务实现类，并注册到 serviceMap 中
    static void serviceImplRegister() throws Exception {
        Reflections reflections = new Reflections("space.ruiwang.serviceimpl");
        Set<Class<?>> annotatedClasses = reflections.getTypesAnnotatedWith(RpcService.class);

        for (Class<?> implClass : annotatedClasses) {
            RpcService annotation = implClass.getAnnotation(RpcService.class);

            // 获取声明要注册的接口
            Class<?> serviceInterface = annotation.service();

            // 如果value是默认值（void.class），则自动获取第一个接口
            if (serviceInterface == void.class) {
                Class<?>[] interfaces = implClass.getInterfaces();
                if (interfaces.length == 0) {
                    throw new IllegalStateException(implClass.getName() + " 未实现任何接口");
                }
                serviceInterface = interfaces[0]; // 取第一个接口
            }

            // 实例化并注册服务
            try {
                Object instance = implClass.getDeclaredConstructor().newInstance();
                SERVICE_MAP.put(serviceInterface.getName(), instance);
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                throw new Exception("接口实现类注册失败: " + implClass.getName(), e);
            }
        }
    }

    static Object getService(String serviceName) {
        return SERVICE_MAP.get(serviceName);
    }
}
