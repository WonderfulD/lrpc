package space.ruiwang.task;

import static space.ruiwang.constants.RedisConstants.SERVICE_CLEANER_KEY;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.TypeReference;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import space.ruiwang.domain.ServiceMetaData;
import space.ruiwang.redisconfig.impl.RedissonOps;
import space.ruiwang.service.ServiceStatus;
import space.ruiwang.serviceregister.RemoteServiceRegisterRedisImpl;
import space.ruiwang.utils.RpcServiceKeyBuilder;

/**
 * @author wangrui <wangrui45@kuaishou.com>
 * Created on 2025-03-09
 */
@Component
@Slf4j
public class ExpiredServiceOfflineTask {
    @Resource
    private RedissonOps redissonOps;

    @Autowired
    private ServiceStatus serviceStatus;

    @Autowired
    private ScheduledExecutorService scheduledExecutor;

    @Autowired
    private RemoteServiceRegisterRedisImpl remoteServiceRegister;

    @PostConstruct
    public void init() {
        // 每10分钟执行一次清理任务，直接调用handle方法
        scheduledExecutor.scheduleAtFixedRate(
                () -> {
                    try {
                        handle();
                    } catch (Exception e) {
                        log.error("执行清理任务时发生异常", e);
                    }
                },
                10,  // 初始延迟
                10, // 间隔时间
                TimeUnit.MINUTES // 时间单位
        );
        log.info("过期服务清理任务调度器已启动");
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

        // 在同一线程池中执行服务清理任务
        for (String serviceKey : serviceKeyList) {
            scheduledExecutor.execute(() -> clean(serviceKey));
        }

        log.info("清理过期服务实例任务已全部提交...");
    }

    private void clean(String serviceKey) {
        try {
            List<ServiceMetaData> serviceList = remoteServiceRegister.search(serviceKey);
            if (CollUtil.isEmpty(serviceList)) {
                log.info("没有需要清理的过期服务实例，服务：[{}]", serviceKey);
                return;
            }
            List<ServiceMetaData> filtered = serviceStatus.filterUnExpired(serviceList);
            String redisKey = RpcServiceKeyBuilder.buildServiceRegisterRedisKey(serviceKey);
            redissonOps.getSet(redisKey, JSONUtil.toJsonStr(filtered));
            if (serviceList.size() != filtered.size()) {
                log.info("清理了{}个过期的实例，服务：[{}]", serviceList.size() - filtered.size(), serviceKey);
            } else {
                log.info("没有需要清理的过期服务实例，服务：[{}]", serviceKey);
            }
        } catch (Exception e) {
            log.error("清理服务[{}]时发生异常", serviceKey, e);
        }
    }

    @PreDestroy
    private void shutdown() {
        // 关闭线程池
        if (scheduledExecutor != null && !scheduledExecutor.isShutdown()) {
            scheduledExecutor.shutdown();
            try {
                // 等待任务完成，最多等待30秒
                if (!scheduledExecutor.awaitTermination(30, TimeUnit.SECONDS)) {
                    scheduledExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduledExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
            log.info("过期服务清理任务线程池已关闭");
        }

        // 删除Redis键
        redissonOps.delete(SERVICE_CLEANER_KEY);
        log.info("过期服务实例清理服务下线，清除Redis对应内容");
    }

}