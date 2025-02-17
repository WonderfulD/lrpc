package space.ruiwang.utils;

import java.io.InputStream;
import java.util.Map;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.yaml.snakeyaml.Yaml;

/**
 * @author wangrui <wangrui45@kuaishou.com>
 * Created on 2025-02-14
 */
public class RedissonManager {
    private static RedissonClient redissonClient;
    private static final String CONFIG_FILE = "config.yml";

    public static RedissonClient getRedissonClient() {
        if (redissonClient == null) {
            Map<String, Object> props = loadYamlConfig();
            Config config = new Config();
            Map<String, Object> redisConfig = (Map<String, Object>) props.get("redis");
            config.useSingleServer()
                    .setAddress((String) redisConfig.get("address"))
                    .setPassword((String) redisConfig.get("password"))
                    .setDatabase((Integer) redisConfig.get("database"))
                    .setConnectionMinimumIdleSize((Integer) redisConfig.get("connectionMinimumIdleSize"))
                    .setConnectionPoolSize((Integer) redisConfig.get("connectionPoolSize"))
                    .setConnectTimeout((Integer) redisConfig.get("connectTimeout"))
                    .setTimeout((Integer) redisConfig.get("timeout"))
                    .setRetryAttempts((Integer) redisConfig.get("retryAttempts"))
                    .setRetryInterval((Integer) redisConfig.get("retryInterval"));
            redissonClient = Redisson.create(config);
        }
        return redissonClient;
    }

    private static Map<String, Object> loadYamlConfig() {
        Yaml yaml = new Yaml();
        try (InputStream inputStream = RedissonManager.class.getClassLoader().getResourceAsStream(CONFIG_FILE)) {
            if (inputStream == null) {
                throw new RuntimeException("Unable to find " + CONFIG_FILE);
            }
            return yaml.load(inputStream);
        } catch (Exception e) {
            throw new RuntimeException("Error loading configuration", e);
        }
    }

    public static void shutdown() {
        if (redissonClient != null && !redissonClient.isShutdown()) {
            redissonClient.shutdown();
        }
    }
}