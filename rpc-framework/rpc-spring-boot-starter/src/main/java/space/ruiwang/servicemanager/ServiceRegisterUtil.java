package space.ruiwang.servicemanager;

import java.lang.reflect.InvocationTargetException;
import java.util.Set;

import org.reflections.Reflections;

import space.ruiwang.annotation.RpcService;

/**
 * 服务实现类注册工具
 * 扫描并注册标注了 @RpcService 的服务实现类
 *
 * @author wangrui <wangrui45@kuaishou.com>
 * Created on 2025-03-10
 */
public class ServiceRegisterUtil {

    /**
     * 扫描指定包下所有标注了 @RpcService 的服务实现类，并注册到 ServiceMapHolder 中
     */
    public static void serviceImplRegister() throws Exception {
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

            // 实例化并注册服务到 ServiceMapHolder
            try {
                Object instance = implClass.getDeclaredConstructor().newInstance();
                ServiceMapHolder.putService(serviceInterface.getName(), instance);
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException
                    | NoSuchMethodException e) {
                throw new Exception("接口实现类注册失败: " + implClass.getName(), e);
            }
        }
    }
}
