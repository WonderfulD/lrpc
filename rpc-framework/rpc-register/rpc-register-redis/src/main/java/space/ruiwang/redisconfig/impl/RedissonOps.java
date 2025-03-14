package space.ruiwang.redisconfig.impl;


import java.util.Collections;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;

import org.redisson.api.RBucket;
import org.redisson.api.RScript;
import org.redisson.api.RScript.Mode;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import space.ruiwang.redisconfig.RedisOpsTemplate;

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
     * 乐观锁解决并发问题：发现要续期的uuid不存在了，就放弃续期
     */
    public boolean renewWithOLock(String key, String uuid, long newEndTime, long extraTTL) {
        RScript rScript = redissonClient.getScript();
        String luaScript = "local key = KEYS[1];"
                         + "local uuid = ARGV[1];"
                         + "local newEndTime = tonumber(ARGV[2]);"
                         + "local extraTTL = tonumber(ARGV[3]);"
                         + "local value = redis.call('GET', key);"
                         + "if not value then return 0 end;"
                         + "local instances = cjson.decode(value);"
                         + "if type(instances) ~= 'table' then return 0 end;"
                         + "local updated = false;"
                         + "for i, inst in ipairs(instances) do "
                         + "    if inst['uuid'] == uuid then "
                         + "        inst['endTime'] = newEndTime; updated = true; break; "
                         + "    end; "
                         + "end; "
                         + "local ttl = redis.call('PTTL', key);"
                         + "if ttl < 0 then ttl = 0 end;"
                         + "if updated then "
                         + "    redis.call('SET', key, cjson.encode(instances));"
                         + "    local newTTL = ttl + extraTTL;"
                         + "    redis.call('PEXPIRE', key, newTTL);"
                         + "    return 1;"
                         + "else return 0;"
                         + "end;";
        Long result = rScript.eval(
                Mode.READ_WRITE,
                luaScript,
                RScript.ReturnType.INTEGER,
                Collections.singletonList(key),  // KEYS[1]
                uuid,                            // ARGV[1]
                String.valueOf(newEndTime),      // ARGV[2]
                String.valueOf(extraTTL)         // ARGV[3]
        );
        // 根据返回结果判断是否续期成功
        return (result != null && result == 1L);
    }

    @Override
    public String getSet(String key, String value) {
        RScript script = redissonClient.getScript();
        String luaScript = "local oldValue = redis.call('GET', KEYS[1])\n"
                         + "local ttl = redis.call('PTTL', KEYS[1])\n"
                         + "if ttl > 0 then\n"
                         + "    redis.call('SET', KEYS[1], ARGV[1], 'PX', ttl)\n"
                         + "else\n"
                         + "    redis.call('SET', KEYS[1], ARGV[1])\n"
                         + "end\n"
                         + "return oldValue";
        // 执行 Lua 脚本（模式为 EVAL，参数为 key + value）
        return script.eval(
                Mode.READ_WRITE,
                luaScript,
                RScript.ReturnType.VALUE,
                Collections.singletonList(key), // KEYS 列表
                value                           // ARGV 列表
        );
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
