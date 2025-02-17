package space.ruiwang.serviceregister.impl;

import static space.ruiwang.constants.RedisConstants.SERVICE_CLUSTER_TTL_MIN;
import static space.ruiwang.constants.RedisConstants.SERVICE_TTL_MIL;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import cn.hutool.core.lang.TypeReference;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import space.ruiwang.domain.ServiceRegisterDO;
import space.ruiwang.serviceregister.ServiceRegister;
import space.ruiwang.utils.RedissonManager;
import space.ruiwang.utils.RpcServiceKeyBuilder;
import space.ruiwang.utils.redisops.impl.RedissonOps;

/**
 * @author wangrui <wangrui45@kuaishou.com>
 * Created on 2025-02-12
 */
@Slf4j
public class RemoteServiceRegister implements ServiceRegister {

    private static final RedissonOps REDISSON_OPS = new RedissonOps(RedissonManager.getRedissonClient());

    // 向Redis注册新服务
    @Override
    public boolean register(ServiceRegisterDO service) {
        String key = RpcServiceKeyBuilder.buildServiceKey(service.getServiceName(), service.getServiceVersion());
        try {
            List<ServiceRegisterDO> registeredServices = null;
            String registeredServicesStr = REDISSON_OPS.get(key);
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
            REDISSON_OPS.setWithExpiration(key, JSONUtil.toJsonStr(registeredServices), SERVICE_CLUSTER_TTL_MIN, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.error("Registering a new Service meets en Error: {}", e.getMessage());
            return false;
        }
        return true;
    }

    // 查找key已注册的实例
    @Override
    public List<ServiceRegisterDO> search(String serviceKey) {
        String registeredServicesStr = REDISSON_OPS.get(serviceKey);
        if (StrUtil.isEmpty(registeredServicesStr)) {
            return null;
        }
        return JSONUtil.toBean(registeredServicesStr, new TypeReference<>() { }, false);
    }
}
