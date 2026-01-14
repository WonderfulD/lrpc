package space.ruiwang.agent.registry.service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.json.Path;
import space.ruiwang.agent.dashscope.DashScopeClient;
import space.ruiwang.agent.registry.config.RedisStackProperties;
import space.ruiwang.agent.registry.config.RegistryHeartbeatProperties;
import space.ruiwang.agent.util.AgentKeyBuilder;
import space.ruiwang.agent.registry.util.AgentIdentityUtil;
import space.ruiwang.domain.agent.AgentCard;
import space.ruiwang.domain.agent.AgentEndpoint;
import space.ruiwang.domain.agent.AgentProvider;
import space.ruiwang.domain.agent.SkillCard;
import space.ruiwang.domain.agent.document.AgentCardDocument;
import space.ruiwang.domain.agent.document.SkillDocument;

@Service
public class AgentRegistryService {
    private final JedisPooled jedis;
    private final DashScopeClient dashScopeClient;
    private final RedisStackProperties properties;
    private final RegistryHeartbeatProperties heartbeatProperties;

    public AgentRegistryService(JedisPooled jedis,
                                DashScopeClient dashScopeClient,
                                RedisStackProperties properties,
                                RegistryHeartbeatProperties heartbeatProperties) {
        this.jedis = jedis;
        this.dashScopeClient = dashScopeClient;
        this.properties = properties;
        this.heartbeatProperties = heartbeatProperties;
    }

    public AgentCard register(AgentCard agent) {
        validateAgentCard(agent);
        String agentId = StrUtil.blankToDefault(agent.getAgentId(), resolveAgentId(agent));
        AgentCardDocument existing = getAgentDocument(agentId);
        if (existing != null && CollUtil.isNotEmpty(existing.getSkills())) {
            for (SkillCard skill : existing.getSkills()) {
                if (StrUtil.isNotBlank(skill.getId())) {
                    jedis.del(AgentKeyBuilder.skillKey(properties.getSkillKeyPrefix(), existing.getAgentId(), skill.getId()));
                }
            }
        }
        agent.setAgentId(agentId);
        agent.setActive(true);
        agent.setLastModifiedTime(nowIso());

        if (agent.getDefaultInputModes() == null) {
            agent.setDefaultInputModes(Collections.emptyList());
        }
        if (agent.getDefaultOutputModes() == null) {
            agent.setDefaultOutputModes(Collections.emptyList());
        }
        if (agent.getSkills() == null) {
            agent.setSkills(new ArrayList<>());
        }

        List<Float> agentEmbedding = dashScopeClient.embed(agent.getDescription());
        AgentCardDocument document = BeanUtil.copyProperties(agent, AgentCardDocument.class);
        document.setEmbedding(agentEmbedding);
        jedis.jsonSet(AgentKeyBuilder.agentKey(properties.getAgentKeyPrefix(), agentId), Path.ROOT_PATH, document);
        ensureIdentityMapping(agent);
        registerAgentId(agentId);
        refreshHeartbeat(agentId);

        for (SkillCard skill : agent.getSkills()) {
            normalizeSkill(agent, skill);
            SkillDocument skillDocument = buildSkillDocument(agent, skill);
            jedis.jsonSet(AgentKeyBuilder.skillKey(properties.getSkillKeyPrefix(), agentId, skill.getId()),
                    Path.ROOT_PATH,
                    skillDocument);
        }
        return agent;
    }

    public void heartbeat(String agentId) {
        if (StrUtil.isBlank(agentId)) {
            throw new IllegalArgumentException("AgentId is required");
        }
        if (getAgentDocument(agentId) == null) {
            throw new IllegalArgumentException("Agent not found: " + agentId);
        }
        registerAgentId(agentId);
        refreshHeartbeat(agentId);
    }

    public void deregister(String agentId) {
        if (StrUtil.isBlank(agentId)) {
            throw new IllegalArgumentException("AgentId is required");
        }
        updateAgentActive(agentId, false);
        jedis.del(AgentKeyBuilder.agentAliveKey(properties.getAgentKeyPrefix(), agentId));
        jedis.del(AgentKeyBuilder.agentHealthFailKey(properties.getAgentKeyPrefix(), agentId));
        jedis.srem(AgentKeyBuilder.agentIdsKey(properties.getAgentKeyPrefix()), agentId);
    }

    public void updateAgentActive(String agentId, boolean active) {
        if (StrUtil.isBlank(agentId)) {
            return;
        }
        String agentKey = AgentKeyBuilder.agentKey(properties.getAgentKeyPrefix(), agentId);
        AgentCardDocument document = jedis.jsonGet(agentKey, AgentCardDocument.class, Path.ROOT_PATH);
        if (document == null) {
            return;
        }
        markAgentActive(document, active);
    }

    public AgentCard getAgent(String agentId) {
        AgentCardDocument document = getAgentDocument(agentId);
        if (document == null) {
            return null;
        }
        return BeanUtil.copyProperties(document, AgentCard.class);
    }

    private AgentCardDocument getAgentDocument(String agentId) {
        if (StrUtil.isBlank(agentId)) {
            return null;
        }
        return jedis.jsonGet(AgentKeyBuilder.agentKey(properties.getAgentKeyPrefix(), agentId),
                AgentCardDocument.class, Path.ROOT_PATH);
    }

    private SkillDocument buildSkillDocument(AgentCard agent, SkillCard skill) {
        SkillDocument document = new SkillDocument();
        document.setAgentId(agent.getAgentId());
        document.setAgentName(agent.getName());
        document.setAgentDescription(agent.getDescription());
        document.setAgentActive(agent.isActive());
        document.setEndpoint(agent.getEndpoint());

        document.setSkillId(skill.getId());
        document.setSkillName(skill.getName());
        document.setSkillDescription(skill.getDescription());
        document.setTags(skill.getTags());
        document.setInputModes(skill.getInputModes());
        document.setOutputModes(skill.getOutputModes());
        document.setVersion(skill.getVersion());
        document.setExamples(skill.getExamples());

        StringBuilder embeddingInput = new StringBuilder();
        if (StrUtil.isNotBlank(skill.getDescription())) {
            embeddingInput.append(skill.getDescription());
        }
        if (CollUtil.isNotEmpty(skill.getTags())) {
            embeddingInput.append("\nTags: ").append(String.join(",", skill.getTags()));
        }
        document.setEmbedding(dashScopeClient.embed(embeddingInput.toString()));
        return document;
    }

    private void normalizeSkill(AgentCard agent, SkillCard skill) {
        if (CollUtil.isEmpty(skill.getInputModes())) {
            skill.setInputModes(agent.getDefaultInputModes());
        }
        if (CollUtil.isEmpty(skill.getOutputModes())) {
            skill.setOutputModes(agent.getDefaultOutputModes());
        }
        if (skill.getTags() == null) {
            skill.setTags(Collections.emptyList());
        }
        if (skill.getExamples() == null) {
            skill.setExamples(Collections.emptyList());
        }
    }

    private void validateAgentCard(AgentCard agent) {
        if (agent == null) {
            throw new IllegalArgumentException("AgentCard is required");
        }
        if (StrUtil.isBlank(agent.getName())) {
            throw new IllegalArgumentException("Agent name is required");
        }
        if (StrUtil.isBlank(agent.getDescription())) {
            throw new IllegalArgumentException("Agent description is required");
        }
        if (StrUtil.isBlank(agent.getVersion())) {
            throw new IllegalArgumentException("Agent version is required");
        }
        AgentProvider provider = agent.getProvider();
        if (provider == null || StrUtil.isBlank(provider.getOrganization()) || StrUtil.isBlank(provider.getUrl())) {
            throw new IllegalArgumentException("Agent provider organization/url is required");
        }
        AgentEndpoint endpoint = agent.getEndpoint();
        if (endpoint == null || StrUtil.isBlank(endpoint.getUrl()) || StrUtil.isBlank(endpoint.getTransport())) {
            throw new IllegalArgumentException("Agent endpoint url/transport is required");
        }
        if (CollUtil.isNotEmpty(agent.getSkills())) {
            for (SkillCard skill : agent.getSkills()) {
                if (StrUtil.isBlank(skill.getId())) {
                    throw new IllegalArgumentException("Skill id is required");
                }
                if (StrUtil.isBlank(skill.getName())) {
                    throw new IllegalArgumentException("Skill name is required");
                }
                if (StrUtil.isBlank(skill.getDescription())) {
                    throw new IllegalArgumentException("Skill description is required");
                }
                if (StrUtil.isBlank(skill.getVersion())) {
                    throw new IllegalArgumentException("Skill version is required");
                }
            }
        }
    }

    private String nowIso() {
        return OffsetDateTime.now(ZoneOffset.ofHours(8)).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }

    private void registerAgentId(String agentId) {
        jedis.sadd(AgentKeyBuilder.agentIdsKey(properties.getAgentKeyPrefix()), agentId);
    }

    private void refreshHeartbeat(String agentId) {
        int ttlSeconds = heartbeatProperties.getTtlSeconds();
        if (ttlSeconds <= 0) {
            ttlSeconds = 90;
        }
        jedis.setex(AgentKeyBuilder.agentAliveKey(properties.getAgentKeyPrefix(), agentId), ttlSeconds, "1");
    }

    private void markAgentActive(AgentCardDocument document, boolean active) {
        if (document == null) {
            return;
        }
        String agentId = document.getAgentId();
        String agentKey = AgentKeyBuilder.agentKey(properties.getAgentKeyPrefix(), agentId);
        jedis.jsonSet(agentKey, Path.of("$.active"), active);
        updateInactiveIndex(agentId, active);
        if (CollUtil.isNotEmpty(document.getSkills())) {
            for (SkillCard skill : document.getSkills()) {
                if (skill == null || StrUtil.isBlank(skill.getId())) {
                    continue;
                }
                String skillKey = AgentKeyBuilder.skillKey(properties.getSkillKeyPrefix(), agentId, skill.getId());
                jedis.jsonSet(skillKey, Path.of("$.agentActive"), active);
            }
        }
    }

    private String resolveAgentId(AgentCard agent) {
        String hash = AgentIdentityUtil.identityHash(agent);
        if (StrUtil.isBlank(hash)) {
            return IdUtil.simpleUUID();
        }
        String identityKey = AgentKeyBuilder.agentIdentityKey(properties.getAgentKeyPrefix(), hash);
        String existing = jedis.get(identityKey);
        if (StrUtil.isNotBlank(existing)) {
            return existing;
        }
        String agentId = IdUtil.simpleUUID();
        Long set = jedis.setnx(identityKey, agentId);
        if (set != null && set == 1L) {
            return agentId;
        }
        String after = jedis.get(identityKey);
        return StrUtil.blankToDefault(after, agentId);
    }

    private void ensureIdentityMapping(AgentCard agent) {
        String hash = AgentIdentityUtil.identityHash(agent);
        if (StrUtil.isBlank(hash) || StrUtil.isBlank(agent.getAgentId())) {
            return;
        }
        String identityKey = AgentKeyBuilder.agentIdentityKey(properties.getAgentKeyPrefix(), hash);
        jedis.setnx(identityKey, agent.getAgentId());
    }

    private void updateInactiveIndex(String agentId, boolean active) {
        String key = AgentKeyBuilder.agentInactiveKey(properties.getAgentKeyPrefix());
        if (active) {
            jedis.zrem(key, agentId);
        } else {
            jedis.zadd(key, System.currentTimeMillis(), agentId);
        }
    }
}
