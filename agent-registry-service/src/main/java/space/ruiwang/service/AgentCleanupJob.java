package space.ruiwang.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.json.Path;
import space.ruiwang.config.RedisStackProperties;
import space.ruiwang.config.RegistryCleanupProperties;
import space.ruiwang.agent.util.AgentKeyBuilder;
import space.ruiwang.domain.agent.SkillCard;
import space.ruiwang.domain.agent.document.AgentCardDocument;

@Slf4j
@Component
public class AgentCleanupJob {
    private final JedisPooled jedis;
    private final RedisStackProperties redisProperties;
    private final RegistryCleanupProperties cleanupProperties;

    public AgentCleanupJob(JedisPooled jedis,
                           RedisStackProperties redisProperties,
                           RegistryCleanupProperties cleanupProperties) {
        this.jedis = jedis;
        this.redisProperties = redisProperties;
        this.cleanupProperties = cleanupProperties;
    }

    @Scheduled(fixedDelayString = "${registry.cleanup.interval-seconds:3600}000")
    public void run() {
        if (!cleanupProperties.isEnabled()) {
            return;
        }
        long ttlMillis = cleanupProperties.getInactiveTtlSeconds() * 1000L;
        if (ttlMillis <= 0) {
            return;
        }
        long cutoff = System.currentTimeMillis() - ttlMillis;
        String inactiveKey = AgentKeyBuilder.agentInactiveKey(redisProperties.getAgentKeyPrefix());
        List<String> expired = jedis.zrangeByScore(inactiveKey, 0, cutoff);
        if (CollUtil.isEmpty(expired)) {
            return;
        }
        for (String agentId : expired) {
            deleteAgent(agentId);
            jedis.zrem(inactiveKey, agentId);
            jedis.srem(AgentKeyBuilder.agentIdsKey(redisProperties.getAgentKeyPrefix()), agentId);
        }
    }

    private void deleteAgent(String agentId) {
        if (StrUtil.isBlank(agentId)) {
            return;
        }
        String agentKey = AgentKeyBuilder.agentKey(redisProperties.getAgentKeyPrefix(), agentId);
        AgentCardDocument document = jedis.jsonGet(agentKey, AgentCardDocument.class, Path.ROOT_PATH);
        List<String> keysToDelete = new ArrayList<>();
        keysToDelete.add(agentKey);
        keysToDelete.add(AgentKeyBuilder.agentAliveKey(redisProperties.getAgentKeyPrefix(), agentId));
        keysToDelete.add(AgentKeyBuilder.agentHealthFailKey(redisProperties.getAgentKeyPrefix(), agentId));
        if (document != null && CollUtil.isNotEmpty(document.getSkills())) {
            for (SkillCard skill : document.getSkills()) {
                if (skill == null || StrUtil.isBlank(skill.getId())) {
                    continue;
                }
                keysToDelete.add(AgentKeyBuilder.skillKey(redisProperties.getSkillKeyPrefix(), agentId, skill.getId()));
            }
        }
        jedis.del(keysToDelete.toArray(new String[0]));
        log.info("Cleaned inactive agent {}", agentId);
    }
}
