package space.ruiwang.utils;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author wangrui <wangrui45@kuaishou.com>
 * Created on 2025-02-14
 */
@Configuration
public class RedissonManager {

    private RedissonClient redissonClient;

    @Value("${redis.address}")
    private String address;

    @Value("${redis.password}")
    private String password;

    @Value("${redis.database}")
    private int database;

    @Value("${redis.connectionMinimumIdleSize}")
    private int connectionMinimumIdleSize;

    @Value("${redis.connectionPoolSize}")
    private int connectionPoolSize;

    @Value("${redis.connectTimeout}")
    private int connectTimeout;

    @Value("${redis.timeout}")
    private int timeout;

    @Value("${redis.retryAttempts}")
    private int retryAttempts;

    @Value("${redis.retryInterval}")
    private int retryInterval;

    @Bean
    public RedissonClient redissonClient() {
        return redissonClient;
    }

    @PostConstruct
    public void init() {
        Config config = new Config();
        config.useSingleServer()
                .setAddress(address)
                .setPassword(password)
                .setDatabase(database)
                .setConnectionMinimumIdleSize(connectionMinimumIdleSize)
                .setConnectionPoolSize(connectionPoolSize)
                .setConnectTimeout(connectTimeout)
                .setTimeout(timeout)
                .setRetryAttempts(retryAttempts)
                .setRetryInterval(retryInterval);
        redissonClient = Redisson.create(config);
    }

    @PreDestroy
    public void shutdown() {
        if (redissonClient != null && !redissonClient.isShutdown()) {
            redissonClient.shutdown();
        }
    }
}