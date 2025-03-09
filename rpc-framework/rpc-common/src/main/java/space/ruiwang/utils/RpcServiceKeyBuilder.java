package space.ruiwang.utils;

import static space.ruiwang.constants.RedisConstants.SERVICE_REGISTER_KEY;

import space.ruiwang.domain.ServiceRegisterDO;

/**
 * @author wangrui <wangrui45@kuaishou.com>
 * Created on 2025-02-12
 */
public class RpcServiceKeyBuilder {
    public static String buildServiceKey(String serviceName, String serviceVersion) {
        return String.join("$", serviceName, serviceVersion);
    }

    public static String buildServiceKey(ServiceRegisterDO service) {
        String serviceName = service.getServiceName();
        String serviceVersion = service.getServiceVersion();
        return buildServiceKey(serviceName, serviceVersion);
    }

    public static String buildServiceRegisterRedisKey(String key) {
        return SERVICE_REGISTER_KEY + key;
    }
}
