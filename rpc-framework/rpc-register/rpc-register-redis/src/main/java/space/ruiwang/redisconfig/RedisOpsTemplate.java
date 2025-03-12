package space.ruiwang.redisconfig;

import java.util.concurrent.TimeUnit;

/**
 * @author wangrui <wangrui45@kuaishou.com>
 * Created on 2025-02-14
 */
public interface RedisOpsTemplate {
    void set(String key, String value);
    void setWithExpiration(String key, String value, long ttl, TimeUnit timeUnit);
    String getSet(String key, String value);
    String get(String key);
    boolean delete(String key);
    boolean hasKey(String key);
    void expire(String key, long ttl, TimeUnit timeUnit);
}
