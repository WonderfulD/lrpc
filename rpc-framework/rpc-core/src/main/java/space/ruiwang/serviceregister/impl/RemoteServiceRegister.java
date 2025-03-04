package space.ruiwang.serviceregister.impl;

import static space.ruiwang.constants.RedisConstants.SERVICE_CLUSTER_TTL_MIN;
import static space.ruiwang.constants.RedisConstants.SERVICE_TTL_MIL;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;

import org.springframework.stereotype.Component;

import cn.hutool.core.lang.TypeReference;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import space.ruiwang.domain.ServiceRegisterDO;
import space.ruiwang.serviceregister.ServiceRegister;
import space.ruiwang.utils.RpcServiceKeyBuilder;
import space.ruiwang.utils.redisops.impl.RedissonOps;

/**
 * @author wangrui <wangrui45@kuaishou.com>
 * Created on 2025-02-12
 */
@Slf4j
@Component
public class RemoteServiceRegister implements ServiceRegister {

    @Resource
    private RedissonOps redissonOps;

    // 向Redis注册新服务
    @Override
    public boolean register(ServiceRegisterDO service) {
        String key = RpcServiceKeyBuilder.buildServiceKey(service.getServiceName(), service.getServiceVersion());
        try {
            List<ServiceRegisterDO> registeredServices = null;
            String registeredServicesStr = redissonOps.get(key);
            if (StrUtil.isEmpty(registeredServicesStr)) {
                // 当前key没有任何服务实例
                registeredServices = new ArrayList<>();
            } else {
                // 当前key已有服务实例
                // 反序列化
                registeredServices = JSONUtil.toBean(registeredServicesStr, new TypeReference<>() { }, false);
            }
            // 若该service未设置过期时间，这里手动设置默认逻辑过期时间
            if (service.getEndTime() == null) {
                service.setEndTime(new Date().getTime() + SERVICE_TTL_MIL);
            }
            registeredServices.add(service);
            redissonOps.setWithExpiration(key, JSONUtil.toJsonStr(registeredServices), SERVICE_CLUSTER_TTL_MIN, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.error("Registering a new Service meets en Error: {}", e.getMessage());
            return false;
        }
        return true;
    }

    /**
     * 取消注册
     * @param service
     * @return
     */
    @Override
    public boolean deregister(ServiceRegisterDO service) {
        String key = RpcServiceKeyBuilder.buildServiceKey(service.getServiceName(), service.getServiceVersion());
        List<ServiceRegisterDO> serviceList = search(key);
        if (serviceList == null) {
            log.warn("远程注册中心：取消注册失败。未找到服务 [{}]", key);
            return false;
        }
        // 过滤出没有当前服务实例的服务实例列表
        ServiceRegisterDO otherService = serviceList.stream().filter(e ->
                        !(e.getServiceAddr().equals(service.getServiceAddr()) && e.getPort().equals(service.getPort())))
                .findFirst().orElse(null);
        // 更新redis
        redissonOps.getSet(key, JSONUtil.toJsonStr(otherService));
        log.info("远程注册中心：服务下线完成。服务 [{}] 下线信息 [{}]",
                key, service);
        return true;
    }

    // 查找key已注册的实例
    @Override
    public List<ServiceRegisterDO> search(String serviceKey) {
        String registeredServicesStr = redissonOps.get(serviceKey);
        if (StrUtil.isEmpty(registeredServicesStr)) {
            return null;
        }
        return JSONUtil.toBean(registeredServicesStr, new TypeReference<>() { }, false);
    }

    @Override
    public boolean renew(ServiceRegisterDO service, Long time, TimeUnit timeUnit) {
        String key = RpcServiceKeyBuilder.buildServiceKey(service.getServiceName(), service.getServiceVersion());
        List<ServiceRegisterDO> foundServices = search(key);
        if (foundServices == null) {
            log.warn("远程注册中心：续约失败。未找到服务 [{}]", key);
            return false;
        }
        ServiceRegisterDO filteredService = foundServices.stream().filter(e -> {
            return e.getServiceAddr().equals(service.getServiceAddr())
                    &&
                    e.getPort().equals(service.getPort());
        }).findFirst().orElse(null);
        if (filteredService == null) {
            log.warn("远程注册中心：续约失败。未找到匹配的服务实例 [{}]", service);
            return false;
        }
        Long endTime = filteredService.getEndTime();
        long micros = timeUnit.toMicros(time);
        filteredService.setEndTime(endTime + micros);
        try {
            redissonOps.setWithExpiration(key, JSONUtil.toJsonStr(foundServices), SERVICE_CLUSTER_TTL_MIN, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.warn("远程注册中心：续约失败。续约时发生错误：[{}]", e.getMessage());
            throw new RuntimeException(e);
        }
        log.info("远程注册中心：服务续约成功。续约时间 [{}] 毫秒: {}", key, micros);
        return true;
    }
}
