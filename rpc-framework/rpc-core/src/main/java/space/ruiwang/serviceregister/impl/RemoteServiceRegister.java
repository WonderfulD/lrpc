package space.ruiwang.serviceregister.impl;

import static space.ruiwang.constants.RedisConstants.SERVICE_CLEANER_KEY;
import static space.ruiwang.constants.RedisConstants.SERVICE_CLUSTER_TTL_MIN;
import static space.ruiwang.constants.RedisConstants.SERVICE_TTL_MIL;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import org.springframework.stereotype.Component;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.TypeReference;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import space.ruiwang.domain.ServiceRegisterDO;
import space.ruiwang.servicemanager.ServiceStatusUtil;
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
    @Resource
    private ServiceStatusUtil serviceStatusUtil;

    /**
     * 向Redis注册新服务
     */
    @Override
    public boolean register(ServiceRegisterDO service) {
        String key = RpcServiceKeyBuilder.buildServiceKey(service.getServiceName(), service.getServiceVersion());
        String redisKey = RpcServiceKeyBuilder.buildServiceRegisterRedisKey(key);
        try {
            List<ServiceRegisterDO> registeredServices;
            String registeredServicesStr = redissonOps.get(redisKey);
            if (StrUtil.isEmpty(registeredServicesStr)) {
                // 当前key没有任何服务实例
                registeredServices = new ArrayList<>();
            } else {
                // 当前key已有服务实例
                // 反序列化
                registeredServices = JSONUtil.toBean(registeredServicesStr, new TypeReference<>() { }, false);
            }

            if (ifRegistered(registeredServices, service)) {
                // 服务实例已被注册过
                throw new RuntimeException("该服务实例已被注册");
            }

            // 若该service未设置过期时间，这里手动设置默认逻辑过期时间
            if (service.getEndTime() == null) {
                service.setEndTime(new Date().getTime() + SERVICE_TTL_MIL);
            }
            registeredServices.add(service);
            redissonOps.setWithExpiration(redisKey, JSONUtil.toJsonStr(registeredServices), SERVICE_CLUSTER_TTL_MIN, TimeUnit.MINUTES);
            add2RegisteredServiceList(service);
            log.info("远程注册中心：服务实例注册成功。服务名: [{}]， 注册信息: [{}]", key, service);
        } catch (Exception e) {
            log.error("远程注册中心：注册服务实例时发生异常。服务名: [{}], 异常信息: [{}]", service.getServiceName(), e.getMessage());
            return false;
        }
        return true;
    }


    /**
     * 服务下线
     */
    @Override
    public boolean deregister(ServiceRegisterDO service) {
        String key = RpcServiceKeyBuilder.buildServiceKey(service.getServiceName(), service.getServiceVersion());
        String redisKey = RpcServiceKeyBuilder.buildServiceRegisterRedisKey(key);
        List<ServiceRegisterDO> serviceList = search(redisKey);
        if (serviceList == null) {
            log.warn("远程注册中心：服务实例下线失败。尝试下线 [{}] 时未找到对应服务实例列表", key);
            return false;
        }
        // 过滤出没有当前服务实例的服务实例列表
        List<ServiceRegisterDO> otherServices = serviceList.stream().filter(e -> !e.getUuid().equals(service.getUuid())).collect(Collectors.toList());
        // 更新redis
        redissonOps.getSet(redisKey, JSONUtil.toJsonStr(otherServices));
        log.info("远程注册中心：服务实例下线完成。服务 [{}] 下线信息 [{}]",
                key, service);
        return true;
    }

    /**
     * 远程注册中心续期
     * 并发风险
     * 查找到需要续期的服务后，丧失CPU执行权，服务实例过期，被下线，此时续期服务还会尝试续期
     * 解决办法：乐观锁
     * 检查UUID是否和查找到的一致，只有一致才续期
     */
    public boolean renew(ServiceRegisterDO service, Long time, TimeUnit timeUnit) {
        String key = RpcServiceKeyBuilder.buildServiceKey(service.getServiceName(), service.getServiceVersion());
        String redisKey = RpcServiceKeyBuilder.buildServiceRegisterRedisKey(key);
        List<ServiceRegisterDO> foundServices = search(redisKey);
        if (CollUtil.isEmpty(foundServices)) {
            log.warn("远程注册中心：续约失败。未找到服务 [{}]", key);
            return false;
        }
        // 查找本服务实例 相同的uuid
        ServiceRegisterDO filteredService = foundServices.stream()
                .filter(e -> e.getUuid().equals(service.getUuid())).findFirst().orElse(null);

        if (filteredService == null) {
            log.warn("远程注册中心：续约失败。未找到匹配的服务实例 [{}]", service);
            return false;
        }

        if (serviceStatusUtil.ifExpired(filteredService)) {
            log.warn("远程注册中心：续约失败。已过期的服务实例 [{}]", service);
            return false;
        }

        String uuid = filteredService.getUuid();
        Long endTime = filteredService.getEndTime();
        long extraTTL = timeUnit.toMillis(time);
        long newEndTime = extraTTL + endTime;
        boolean renewed = redissonOps.renewWithOLock(redisKey, uuid, newEndTime, extraTTL);
        if (!renewed) {
            log.warn("远程注册中心：续约失败，实例不存在或UUID不匹配");
            return false;
        } else {
            log.info("远程注册中心：服务实例续约成功。续约时间 [{}] 毫秒: {}", key, extraTTL);
            return true;
        }
    }

    /**
     * 查找key已注册的实例
     */
    @Override
    public List<ServiceRegisterDO> search(String serviceKey) {
        String registeredServicesStr = redissonOps.get(serviceKey);
        if (StrUtil.isEmpty(registeredServicesStr)) {
            return null;
        }
        return JSONUtil.toBean(registeredServicesStr, new TypeReference<>() { }, false);
    }

    public void add2RegisteredServiceList(ServiceRegisterDO service) {
        Set<String> serviceKeyList;
        String serviceKeysStr = redissonOps.get(SERVICE_CLEANER_KEY);
        if (StrUtil.isEmpty(serviceKeysStr)) {
            serviceKeyList = new HashSet<>();
        } else {
            serviceKeyList = JSONUtil.toBean(serviceKeysStr, new TypeReference<>() { }, false);
        }
        serviceKeyList.add(RpcServiceKeyBuilder.buildServiceKey(service));
        redissonOps.set(SERVICE_CLEANER_KEY, JSONUtil.toJsonStr(serviceKeyList));
    }
}
