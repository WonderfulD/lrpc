package space.ruiwang.serviceregister;

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

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.TypeReference;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import space.ruiwang.api.serviceregister.sub.IRemoteServiceRegister;
import space.ruiwang.domain.ServiceMetaData;
import space.ruiwang.redisconfig.impl.RedissonOps;
import space.ruiwang.service.ServiceStatus;
import space.ruiwang.servicemanager.ServiceRegisterUtil;
import space.ruiwang.utils.RpcServiceKeyBuilder;


/**
 * @author wangrui <wangrui45@kuaishou.com>
 * Created on 2025-02-12
 */
@Slf4j
@Lazy
@Component
public class RemoteServiceRegisterRedisImpl implements IRemoteServiceRegister {

    @Resource
    private RedissonOps redissonOps;
    @Resource
    private ServiceStatus serviceStatus;

    /**
     * 向Redis注册新服务
     */
    @Override
    public boolean register(ServiceMetaData service) {
        String key = RpcServiceKeyBuilder.buildServiceKey(service);
        String redisKey = RpcServiceKeyBuilder.buildServiceRegisterRedisKey(key);
        try {
            List<ServiceMetaData> registeredServices;
            String registeredServicesStr = redissonOps.get(redisKey);
            if (StrUtil.isEmpty(registeredServicesStr)) {
                // 当前key没有任何服务实例
                registeredServices = new ArrayList<>();
            } else {
                // 当前key已有服务实例
                // 反序列化
                registeredServices = JSONUtil.toBean(registeredServicesStr, new TypeReference<>() { }, false);
            }

            if (ServiceRegisterUtil.ifRegistered(registeredServices, service)) {
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
    public boolean deregister(ServiceMetaData service) {
        String key = RpcServiceKeyBuilder.buildServiceKey(service.getServiceName(), service.getServiceVersion());
        List<ServiceMetaData> serviceList = search(key);
        if (serviceList == null) {
            log.warn("远程注册中心：服务实例下线失败。尝试下线 [{}] 时未找到对应服务实例列表", key);
            return false;
        }
        // 过滤出没有当前服务实例的服务实例列表
        List<ServiceMetaData> otherServices = serviceList.stream().filter(e -> !e.getUuid().equals(service.getUuid())).collect(Collectors.toList());
        // 更新redis
        String redisKey = RpcServiceKeyBuilder.buildServiceRegisterRedisKey(key);
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
    public boolean renew(ServiceMetaData service, Long time, TimeUnit timeUnit) {
        String key = RpcServiceKeyBuilder.buildServiceKey(service.getServiceName(), service.getServiceVersion());
        List<ServiceMetaData> foundServices = search(key);
        if (CollUtil.isEmpty(foundServices)) {
            log.warn("远程注册中心：续约失败。未找到服务 [{}]", key);
            return false;
        }
        // 查找本服务实例 相同的uuid
        ServiceMetaData filteredService = foundServices.stream()
                .filter(e -> e.getUuid().equals(service.getUuid())).findFirst().orElse(null);

        if (filteredService == null) {
            log.warn("远程注册中心：续约失败。未找到匹配的服务实例 [{}]", service);
            return false;
        }

        if (serviceStatus.ifExpired(filteredService)) {
            log.warn("远程注册中心：续约失败。已过期的服务实例 [{}]", service);
            return false;
        }

        String uuid = filteredService.getUuid();
        Long endTime = filteredService.getEndTime();
        long extraTTL = timeUnit.toMillis(time);
        long newEndTime = extraTTL + endTime;
        String redisKey = RpcServiceKeyBuilder.buildServiceRegisterRedisKey(key);
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
    public List<ServiceMetaData> search(String serviceKey) {
        String redisKey = RpcServiceKeyBuilder.buildServiceRegisterRedisKey(serviceKey);
        String registeredServicesStr = redissonOps.get(redisKey);
        if (StrUtil.isEmpty(registeredServicesStr)) {
            return null;
        }
        List<ServiceMetaData> serviceList = JSONUtil.toBean(registeredServicesStr, new TypeReference<>() { }, false);
        return filterUnExpiredServiceList(serviceList);
    }

    public void add2RegisteredServiceList(ServiceMetaData service) {
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

    /**
     * 获取未过期服务列表
     */
    private List<ServiceMetaData> filterUnExpiredServiceList(List<ServiceMetaData> serviceList) {
        if (CollUtil.isEmpty(serviceList)) {
            return new ArrayList<>();
        }
        return serviceStatus.filterUnExpired(serviceList);
    }
}
