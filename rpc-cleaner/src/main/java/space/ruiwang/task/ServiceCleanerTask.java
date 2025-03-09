package space.ruiwang.task;

import static space.ruiwang.constants.RedisConstants.SERVICE_CLEANER_KEY;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.PreDestroy;
import javax.annotation.Resource;

import org.springframework.scheduling.annotation.Scheduled;
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
 * Created on 2025-03-09
 */
@Component
@Slf4j
public class ServiceCleanerTask {
    @Resource
    private RedissonOps redissonOps;
    @Resource(name = "remoteServiceRegister")
    private ServiceRegister remoteServiceRegister;
    @Resource
    private ServiceStatusUtil serviceStatusUtil;
    @Scheduled(cron = "0 */10 * * * *")
    public void taskWithCron() {
        handle();
    }

    private void handle() {
        log.info("开始清理过期服务实例...");
        Set<String> serviceKeyList;
        String serviceKeysStr = redissonOps.get(SERVICE_CLEANER_KEY);
        if (StrUtil.isEmpty(serviceKeysStr)) {
            serviceKeyList = new HashSet<>();
        } else {
            serviceKeyList = JSONUtil.toBean(serviceKeysStr, new TypeReference<>() { }, false);
        }
        if (CollUtil.isEmpty(serviceKeyList)) {
            log.warn("没有要清理的服务");
            return;
        }
        serviceKeyList.forEach(this::clean);
        log.info("清理过期服务实例完成...");
    }

    private void clean(String serviceKey) {
        String redisKey = RpcServiceKeyBuilder.buildServiceRegisterRedisKey(serviceKey);
        List<ServiceRegisterDO> serviceList = remoteServiceRegister.search(redisKey);
        List<ServiceRegisterDO> filtered = serviceStatusUtil.filterUnExpired(serviceList);
        redissonOps.getSet(redisKey, JSONUtil.toJsonStr(filtered));
        if (serviceList.size() != filtered.size()) {
            log.info("清理了{}个过期的实例，服务：[{}]", serviceList.size() - filtered.size(), serviceKey);
        } else {
            log.info("没有需要清理的过期服务实例，服务：[{}]", serviceKey);
        }
    }

    @PreDestroy
    private void shutdown() {
        redissonOps.delete(SERVICE_CLEANER_KEY);
        log.info("过期服务实例清理服务下线，清除Redis对应内容");
    }
}
