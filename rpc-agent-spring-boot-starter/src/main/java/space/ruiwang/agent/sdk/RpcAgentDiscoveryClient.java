package space.ruiwang.agent.sdk;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

import space.ruiwang.domain.agent.LlmSkill;
import space.ruiwang.domain.agent.dto.AgentDiscoverRequest;
import space.ruiwang.domain.agent.dto.AgentDiscoverResponse;

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
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("query is required");
        }
        AgentDiscoverRequest request = new AgentDiscoverRequest();
        request.setQuery(query.trim());
        if (maxSkills > 0) {
            request.setTopK(maxSkills);
        }
        if (minScore > 0) {
            request.setMinScore(minScore);
        }
        String agentId = registrationState.getAgentId();
        if (agentId != null && !agentId.isBlank()) {
            request.setCallerAgentId(agentId);
            request.setExcludeAgentIds(List.of(agentId));
        }
        AgentDiscoverResponse response = client.post()
                .uri(properties.getDiscoverPath())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(AgentDiscoverResponse.class)
                .block(DEFAULT_TIMEOUT);
        if (response == null || response.getSkills() == null) {
            return Collections.emptyList();
        }
        return response.getSkills();
    }
}
