package space.ruiwang.utils.redisops.impl;

import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;

import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import space.ruiwang.utils.redisops.RedisOpsTemplate;

/**
 * @author wangrui <wangrui45@kuaishou.com>
 * Created on 2025-02-14
 */
@Component
public class RedissonOps implements RedisOpsTemplate {
    @Resource
    private RedissonClient redissonClient;

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

    /**
     * 操作非原子，可能导致set后生存时间被重置，而非剩余时间
     */
    @Override
    public void getSet(String key, String value) {
        RBucket<String> bucket = redissonClient.getBucket(key);
        long remainingTTL = bucket.remainTimeToLive();
        if (remainingTTL > 0) {
            bucket.getAndSet(value, remainingTTL, TimeUnit.MILLISECONDS);
        } else {
            bucket.getAndSet(value);
        }
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
