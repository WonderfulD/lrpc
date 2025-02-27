package space.ruiwang.utils.redisops.impl;

import java.util.concurrent.TimeUnit;

import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;

import space.ruiwang.utils.redisops.RedisOpsTemplate;

/**
 * @author wangrui <wangrui45@kuaishou.com>
 * Created on 2025-02-14
 */
public class RedissonOps implements RedisOpsTemplate {

    private final RedissonClient redissonClient;

    public RedissonOps(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    @Override
    public void set(String key, String value) {
        RBucket<String> bucket = redissonClient.getBucket(key);
        bucket.set(value);
    }

    @Override
    public void setWithExpiration(String key, String value, long ttl, TimeUnit timeUnit) {
        RBucket<String> bucket = redissonClient.getBucket(key);
        bucket.set(value, ttl, timeUnit);
    }

    @Override
    public String get(String key) {
        RBucket<String> bucket = redissonClient.getBucket(key);
        return bucket.get();
    }

    @Override
    public boolean delete(String key) {
        return redissonClient.getBucket(key).delete();
    }

    @Override
    public boolean hasKey(String key) {
        return redissonClient.getBucket(key).isExists();
    }

    @Override
    public void expire(String key, long ttl, TimeUnit timeUnit) {
        redissonClient.getBucket(key).expire(ttl, timeUnit);
    }
}
