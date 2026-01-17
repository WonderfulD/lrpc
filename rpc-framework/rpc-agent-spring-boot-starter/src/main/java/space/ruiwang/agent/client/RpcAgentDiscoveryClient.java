package space.ruiwang.agent.client;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

import space.ruiwang.agent.config.RpcAgentProperties;
import space.ruiwang.agent.registration.AgentRegistrationState;
import space.ruiwang.domain.agent.LlmSkill;
import space.ruiwang.domain.agent.dto.AgentDiscoverRequest;
import space.ruiwang.domain.agent.dto.AgentDiscoverResponse;
import space.ruiwang.domain.agent.invoke.AgentInvokeRequest;

public class RpcAgentDiscoveryClient {
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(15);

    private final RpcAgentProperties properties;
    private final AgentRegistrationState registrationState;
    private final WebClient client;

    public RpcAgentDiscoveryClient(RpcAgentProperties properties,
                                   WebClient.Builder builder,
                                   AgentRegistrationState registrationState) {
        this.properties = properties;
        this.registrationState = registrationState;
        this.client = builder.baseUrl(properties.getDiscoveryBaseUrl()).build();
    }

    public List<LlmSkill> discover(String query, int maxSkills, double minScore) {
        AgentInvokeRequest invokeRequest = new AgentInvokeRequest();
        invokeRequest.setQuery(query);
        if (maxSkills > 0) {
            invokeRequest.setMaxSkills(maxSkills);
        }
        if (minScore > 0) {
            invokeRequest.setMinScore(minScore);
        }
        return discover(invokeRequest);
    }

    public List<LlmSkill> discover(AgentInvokeRequest invokeRequest) {
        if (invokeRequest == null || invokeRequest.getQuery() == null || invokeRequest.getQuery().isBlank()) {
            throw new IllegalArgumentException("query is required");
        }
        AgentDiscoverRequest request = new AgentDiscoverRequest();
        request.setQuery(invokeRequest.getQuery().trim());
        if (invokeRequest.getMaxSkills() != null && invokeRequest.getMaxSkills() > 0) {
            request.setTopK(invokeRequest.getMaxSkills());
        }
        if (invokeRequest.getMinScore() != null && invokeRequest.getMinScore() > 0) {
            request.setMinScore(invokeRequest.getMinScore());
        }
        String callerAgentId = invokeRequest.getCallerAgentId();
        if (callerAgentId == null || callerAgentId.isBlank()) {
            callerAgentId = registrationState.getAgentId();
        }
        List<String> excluded = new ArrayList<>();
        if (invokeRequest.getExcludeAgentIds() != null) {
            excluded.addAll(invokeRequest.getExcludeAgentIds());
        }
        if (callerAgentId != null && !callerAgentId.isBlank()) {
            request.setCallerAgentId(callerAgentId);
            if (!excluded.contains(callerAgentId)) {
                excluded.add(callerAgentId);
            }
        }
        if (!excluded.isEmpty()) {
            request.setExcludeAgentIds(excluded);
        }
        Duration timeout = resolveTimeout(invokeRequest);
        AgentDiscoverResponse response = client.post()
                .uri(properties.getDiscoverPath())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(AgentDiscoverResponse.class)
                .block(timeout);
        if (response == null || response.getSkills() == null) {
            return Collections.emptyList();
        }
        return response.getSkills();
    }

    private Duration resolveTimeout(AgentInvokeRequest request) {
        if (request != null && request.getTimeoutMs() != null && request.getTimeoutMs() > 0) {
            return Duration.ofMillis(request.getTimeoutMs());
        }
        return DEFAULT_TIMEOUT;
    }
}
