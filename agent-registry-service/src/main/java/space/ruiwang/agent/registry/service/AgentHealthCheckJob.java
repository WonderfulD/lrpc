package space.ruiwang.agent.registry.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Set;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.json.Path;
import space.ruiwang.agent.registry.config.RedisStackProperties;
import space.ruiwang.agent.registry.config.RegistryHealthCheckProperties;
import space.ruiwang.agent.util.AgentKeyBuilder;
import space.ruiwang.domain.agent.AgentEndpoint;
import space.ruiwang.domain.agent.document.AgentCardDocument;

@Slf4j
@Component
public class AgentHealthCheckJob {
    private final JedisPooled jedis;
    private final RedisStackProperties redisProperties;
    private final RegistryHealthCheckProperties healthProperties;
    private final HttpClient httpClient;
    private final AgentRegistryService registryService;

    public AgentHealthCheckJob(JedisPooled jedis,
                               RedisStackProperties redisProperties,
                               RegistryHealthCheckProperties healthProperties,
                               AgentRegistryService registryService) {
        this.jedis = jedis;
        this.redisProperties = redisProperties;
        this.healthProperties = healthProperties;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(healthProperties.getTimeoutMs()))
                .build();
        this.registryService = registryService;
    }

    @Scheduled(fixedDelayString = "${registry.health-check.interval-seconds:30}000")
    public void run() {
        if (!healthProperties.isEnabled()) {
            return;
        }
        String agentIdsKey = AgentKeyBuilder.agentIdsKey(redisProperties.getAgentKeyPrefix());
        Set<String> agentIds = jedis.smembers(agentIdsKey);
        if (CollUtil.isEmpty(agentIds)) {
            return;
        }
        for (String agentId : agentIds) {
            checkAgent(agentId);
        }
    }

    private void checkAgent(String agentId) {
        if (StrUtil.isBlank(agentId)) {
            return;
        }
        String aliveKey = AgentKeyBuilder.agentAliveKey(redisProperties.getAgentKeyPrefix(), agentId);
        if (!jedis.exists(aliveKey)) {
            registryService.updateAgentActive(agentId, false);
            return;
        }
        AgentCardDocument document = jedis.jsonGet(
                AgentKeyBuilder.agentKey(redisProperties.getAgentKeyPrefix(), agentId),
                AgentCardDocument.class,
                Path.ROOT_PATH);
        if (document == null) {
            return;
        }
        if (document.getEndpoint() == null) {
            registryService.updateAgentActive(agentId, false);
            return;
        }
        boolean healthy = checkHealth(document.getEndpoint());
        if (healthy) {
            resetFailure(agentId);
            registryService.updateAgentActive(agentId, true);
            return;
        }
        long failures = incrementFailure(agentId);
        if (failures >= healthProperties.getFailureThreshold()) {
            registryService.updateAgentActive(agentId, false);
        }
    }

    private boolean checkHealth(AgentEndpoint endpoint) {
        URI uri = buildHealthUri(endpoint);
        if (uri == null) {
            return false;
        }
        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .timeout(Duration.ofMillis(healthProperties.getTimeoutMs()))
                .GET()
                .build();
        try {
            HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            return response.statusCode() / 100 == 2;
        } catch (Exception e) {
            log.debug("Health check failed for {}: {}", endpoint.getUrl(), e.getMessage());
            return false;
        }
    }

    private URI buildHealthUri(AgentEndpoint endpoint) {
        if (endpoint == null || StrUtil.isBlank(endpoint.getUrl())) {
            return null;
        }
        URI endpointUri;
        try {
            endpointUri = URI.create(endpoint.getUrl());
        } catch (Exception e) {
            return null;
        }
        String scheme = endpointUri.getScheme();
        String host = endpointUri.getHost();
        if (StrUtil.isBlank(scheme) || StrUtil.isBlank(host)) {
            return null;
        }
        StringBuilder base = new StringBuilder();
        base.append(scheme).append("://").append(host);
        int port = endpointUri.getPort();
        if (port > 0) {
            base.append(":").append(port);
        }
        String path = healthProperties.getHealthPath();
        if (StrUtil.isBlank(path)) {
            path = "/lrpc/agent/health";
        }
        if (!path.startsWith("/")) {
            base.append("/");
        }
        base.append(path);
        return URI.create(base.toString());
    }

    private long incrementFailure(String agentId) {
        String key = AgentKeyBuilder.agentHealthFailKey(redisProperties.getAgentKeyPrefix(), agentId);
        long count = jedis.incr(key);
        jedis.expire(key, healthProperties.getIntervalSeconds() * 5L);
        return count;
    }

    private void resetFailure(String agentId) {
        String key = AgentKeyBuilder.agentHealthFailKey(redisProperties.getAgentKeyPrefix(), agentId);
        jedis.del(key);
    }

}