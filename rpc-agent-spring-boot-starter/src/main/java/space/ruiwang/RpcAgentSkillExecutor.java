package space.ruiwang;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.annotation.PreDestroy;

import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

import lombok.extern.slf4j.Slf4j;
import space.ruiwang.constants.FaultTolerantStrategies;
import space.ruiwang.constants.LoadBalancerStrategies;
import space.ruiwang.domain.RpcRequestConfig;
import space.ruiwang.domain.agent.AgentEndpoint;
import space.ruiwang.domain.agent.LlmSkill;
import space.ruiwang.domain.agent.invoke.AgentSkillInvokeRequest;
import space.ruiwang.domain.agent.invoke.AgentSkillInvokeResponse;
import space.ruiwang.domain.agent.invoke.AgentSkillInvokeResult;
import space.ruiwang.domain.agent.invoke.AgentSkillService;
import space.ruiwang.proxy.ProxyAgent;

@Slf4j
public class RpcAgentSkillExecutor {
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(15);

    private final ProxyAgent proxyAgent;
    private final WebClient.Builder builder;
    private final ExecutorService executor;

    public RpcAgentSkillExecutor(ProxyAgent proxyAgent, WebClient.Builder builder) {
        this.proxyAgent = proxyAgent;
        this.builder = builder;
        this.executor = Executors.newFixedThreadPool(8);
    }

    public List<AgentSkillInvokeResult> executeParallel(List<LlmSkill> skills, String query, Duration timeout) {
        if (skills == null || skills.isEmpty()) {
            return List.of();
        }
        Duration resolved = resolveTimeout(timeout);
        List<CompletableFuture<AgentSkillInvokeResult>> futures = new ArrayList<>();
        for (LlmSkill skill : skills) {
            futures.add(CompletableFuture.supplyAsync(() -> execute(skill, query, resolved), executor));
        }
        List<AgentSkillInvokeResult> results = new ArrayList<>();
        for (CompletableFuture<AgentSkillInvokeResult> future : futures) {
            try {
                AgentSkillInvokeResult result =
                        future.get(resolved.toMillis(), TimeUnit.MILLISECONDS);
                if (result != null) {
                    results.add(result);
                }
            } catch (Exception e) {
                log.warn("Skill execution failed", e);
                future.cancel(true);
            }
        }
        return results;
    }

    public AgentSkillInvokeResult execute(LlmSkill skill, String query, Duration timeout) {
        if (skill == null || skill.getEndpoint() == null) {
            throw new IllegalArgumentException("Skill endpoint is required");
        }
        Duration resolved = resolveTimeout(timeout);
        AgentSkillInvokeRequest request = new AgentSkillInvokeRequest();
        request.setSkillId(skill.getSkillId());
        request.setInput(query);
        request.setQuery(query);

        AgentEndpoint endpoint = skill.getEndpoint();
        List<String> transports = endpoint.getTransport();
        boolean supportsRpc = supportsTransport(transports, "RPC");
        boolean supportsHttp = transports == null || transports.isEmpty() || supportsTransport(transports, "HTTP");

        if (supportsRpc) {
            try {
                AgentSkillInvokeResponse response = invokeByRpc(skill, request, resolved);
                return toResult(skill, response);
            } catch (Exception e) {
                log.warn("RPC invoke failed, fallback to HTTP if available", e);
                if (!supportsHttp) {
                    throw e;
                }
            }
        }
        if (supportsHttp) {
            AgentSkillInvokeResponse response = invokeByHttp(endpoint.getUrl(), request, resolved);
            return toResult(skill, response);
        }
        throw new IllegalStateException("No supported transport for skill " + skill.getSkillId());
    }

    private AgentSkillInvokeResponse invokeByRpc(LlmSkill skill, AgentSkillInvokeRequest request, Duration timeout) {
        String organization = skill.getAgentOrganization();
        String name = skill.getAgentName();
        String version = skill.getAgentVersion();
        if (organization == null || organization.isBlank()
                || name == null || name.isBlank()
                || version == null || version.isBlank()) {
            throw new IllegalStateException("Missing agent organization/name/version for RPC invoke");
        }
        String serviceName = organization.trim() + "@" + name.trim();
        RpcRequestConfig config = new RpcRequestConfig(
                LoadBalancerStrategies.CONSISTENT_HASHING,
                1,
                timeout.toMillis(),
                FaultTolerantStrategies.FAIL_FAST
        );
        AgentSkillService service = proxyAgent.getProxy(AgentSkillService.class, serviceName, version.trim(), config);
        return service.invoke(request);
    }

    private AgentSkillInvokeResponse invokeByHttp(String url, AgentSkillInvokeRequest request, Duration timeout) {
        if (url == null || url.isBlank()) {
            throw new IllegalStateException("HTTP endpoint url is required");
        }
        return builder.build()
                .post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(AgentSkillInvokeResponse.class)
                .block(timeout);
    }

    private boolean supportsTransport(List<String> transports, String target) {
        if (transports == null || transports.isEmpty()) {
            return false;
        }
        for (String transport : transports) {
            if (transport == null || transport.isBlank()) {
                continue;
            }
            for (String part : transport.split(",")) {
                if (part.trim().equalsIgnoreCase(target)) {
                    return true;
                }
            }
        }
        return false;
    }

    private AgentSkillInvokeResult toResult(LlmSkill skill, AgentSkillInvokeResponse response) {
        if (response == null) {
            return null;
        }
        AgentSkillInvokeResult result = new AgentSkillInvokeResult();
        result.setAgentId(skill.getAgentId());
        result.setSkillId(response.getSkillId());
        result.setScore(skill.getScore());
        result.setResult(response.getResult());
        return result;
    }

    private Duration resolveTimeout(Duration timeout) {
        if (timeout != null) {
            return timeout;
        }
        return DEFAULT_TIMEOUT;
    }

    @PreDestroy
    public void shutdown() {
        executor.shutdown();
    }
}
