package space.ruiwang.agent.client;

import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

import reactor.core.publisher.Mono;
import space.ruiwang.agent.config.RpcAgentProperties;
import space.ruiwang.domain.agent.AgentCard;
import space.ruiwang.domain.agent.dto.AgentDeregisterRequest;
import space.ruiwang.domain.agent.dto.AgentHeartbeatRequest;

public class RpcAgentClient {
    private final WebClient webClient;
    private final RpcAgentProperties properties;

    public RpcAgentClient(RpcAgentProperties properties) {
        this.properties = properties;
        this.webClient = WebClient.builder()
                .baseUrl(properties.getRegistryBaseUrl())
                .build();
    }

    public Mono<AgentCard> register(AgentCard agent) {
        return webClient.post()
                .uri(properties.getRegisterPath())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(agent)
                .retrieve()
                .bodyToMono(AgentCard.class);
    }

    public Mono<Void> heartbeat(String agentId) {
        AgentHeartbeatRequest request = new AgentHeartbeatRequest();
        request.setAgentId(agentId);
        return webClient.post()
                .uri(properties.getHeartbeatPath())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(Void.class);
    }

    public Mono<Void> deregister(String agentId) {
        AgentDeregisterRequest request = new AgentDeregisterRequest();
        request.setAgentId(agentId);
        return webClient.post()
                .uri(properties.getDeregisterPath())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(Void.class);
    }
}
