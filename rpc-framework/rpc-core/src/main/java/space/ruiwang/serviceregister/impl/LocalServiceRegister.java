package space.ruiwang.serviceregister.impl;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.annotation.Resource;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import space.ruiwang.domain.ServiceRegisterDO;
import space.ruiwang.servicemanager.ServiceLoaderUtil;
import space.ruiwang.serviceregister.ServiceRegister;
import space.ruiwang.utils.RpcServiceKeyBuilder;

/**
 * @author wangrui <wangrui45@kuaishou.com>
 * Created on 2025-02-11
 */


/**
 * 本地注册中心
 * 1. 使用并发安全的容器
 * 2. 支持服务的注册、查询、下线
 * 3. 仅示例用途；实际可与分布式注册中心协同
 */
@Slf4j
@Component
public class LocalServiceRegister implements ServiceRegister {

    /**
     * 线程安全的本地注册表
     * Key: 服务名 + 服务版本 (ServiceName$ServiceVersion)
     * Value: 该服务对应的多个ServiceRegisterDO对象列表（不同实例的服务）
     *
     * 说明:
     *   - ConcurrentHashMap 保证Map本身的线程安全
     *   - CopyOnWriteArrayList 在增删时会复制底层数组，读操作时无锁
     *   - 在写操作不算非常频繁的场景下，可简化并发处理
     */
    private static final Map<String, CopyOnWriteArrayList<ServiceRegisterDO>> LOCAL_REGISTRATION = new ConcurrentHashMap<>();

    @Resource
    private ServiceLoaderUtil serviceLoaderUtil;


    /**
     * 注册服务到本地
     * 即拉取远程服务注册中心对应服务的镜像
     *
     * @param serviceRegisterDO 服务描述对象 (包含服务的详细信息)
     * @return 注册成功返回true, 失败返回false
     */
    @Override
    public boolean register(ServiceRegisterDO serviceRegisterDO) {
        String serviceName = serviceRegisterDO.getServiceName();
        String serviceVersion = serviceRegisterDO.getServiceVersion();
        return loadService(serviceName, serviceVersion);
    }

    /**
     * 将某个具体实现的服务从本地注册中心移除
     * 例如某服务下线时使用
     *
     * @param serviceRegisterDO 需要移除的ServiceRegisterDO
     * @return 下线成功或失败
     */
    public boolean deregister(ServiceRegisterDO serviceRegisterDO) {
        try {
            String key = RpcServiceKeyBuilder.buildServiceKey(serviceRegisterDO.getServiceName(), serviceRegisterDO.getServiceVersion());
            CopyOnWriteArrayList<ServiceRegisterDO> serviceList = LOCAL_REGISTRATION.get(key);
            if (serviceList != null) {
                // 移除指定ServiceRegisterDO
                boolean removed = serviceList.remove(serviceRegisterDO);

                // 如果该服务下已无ServiceRegisterDO, 就把这个Key也移除
                if (serviceList.isEmpty()) {
                    LOCAL_REGISTRATION.remove(key);
                }

                log.info("本地注册中心：服务实例下线完成。服务 [{}] 下线信息 [{}]", key, serviceRegisterDO);

                return removed;
            } else {
                log.warn("本地注册中心：服务实例下线失败。尝试下线 [{}] 时未找到对应服务实例列表", key);
                return false;
            }
        } catch (Exception e) {
            log.error("本地注册中心：服务下线时发生异常。服务名: [{}], 异常信息: [{}]", serviceRegisterDO.getServiceName(), e.getMessage());
            return false;
        }
    }

    /**
     * 拉取远程注册中心数据
     */
    public boolean loadService(String serviceName, String serviceVersion) {
        String serviceKey = RpcServiceKeyBuilder.buildServiceKey(serviceName, serviceVersion);
        try {
            CopyOnWriteArrayList<ServiceRegisterDO> serviceList = new CopyOnWriteArrayList<>(serviceLoaderUtil.loadService(serviceKey));
            LOCAL_REGISTRATION.put(serviceKey, serviceList);
            log.info("从远程注册中心拉取服务实例列表[{}]成功", serviceKey);
            return true;
        } catch (Exception e) {
            log.error("从远程注册中心拉取服务实例列表[{}]失败, 错误信息: [{}]", serviceKey, e.getMessage());
            return false;
        }
    }

    /**
     * 查找本地注册中心中某服务对应的ServiceRegisterDO列表
     *
     * @param serviceKey 服务Key
     * @return ServiceRegisterDO列表；可能为null或空，如果未找到
     */
    @Override
    public List<ServiceRegisterDO> search(String serviceKey) {
        return LOCAL_REGISTRATION.get(serviceKey);
    }
}
