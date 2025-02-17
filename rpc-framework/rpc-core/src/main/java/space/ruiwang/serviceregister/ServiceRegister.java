package space.ruiwang.serviceregister;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.reflections.Reflections;

import space.ruiwang.domain.ServiceRegisterDO;
import space.ruiwang.references.RpcService;

/**
 * @author wangrui <wangrui45@kuaishou.com>
 * Created on 2025-02-11
 */
public interface ServiceRegister {
    // 使用线程安全的 Map 存储服务接口名与对应的服务实例
    Map<String, Object> SERVICE_MAP = new ConcurrentHashMap<>();

    boolean register(ServiceRegisterDO service);

    List<ServiceRegisterDO> search(String serviceKey);

    // 扫描指定包下所有标注了 @RpcService 的服务实现类，并注册到 serviceMap 中
    static void serviceImplRegister() throws Exception {
        // 扫描指定包下所有标注了 @RpcService 的类
        Reflections reflections = new Reflections("space.ruiwang.serviceimpl");
        Set<Class<?>> annotatedClasses = reflections.getTypesAnnotatedWith(RpcService.class);
        for (Class<?> implClass : annotatedClasses) {
            RpcService annotation = implClass.getAnnotation(RpcService.class);
            Class<?> serviceInterface = annotation.value();  // 获取实现的接口
            try {
                Object instance = implClass.getDeclaredConstructor().newInstance();
                SERVICE_MAP.put(serviceInterface.getName(), instance);
            } catch (Exception e) {
                throw new Exception("接口实现类注册失败");
            }
        }
    }

    static Object getService(String serviceName) {
        return SERVICE_MAP.get(serviceName);
    }
}
