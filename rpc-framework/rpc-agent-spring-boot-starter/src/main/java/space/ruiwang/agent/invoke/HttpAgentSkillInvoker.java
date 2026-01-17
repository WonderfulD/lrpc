package space.ruiwang.agent.invoke;

import java.time.Duration;
import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

import lombok.extern.slf4j.Slf4j;
import space.ruiwang.domain.agent.AgentEndpoint;
import space.ruiwang.domain.agent.LlmSkill;
import space.ruiwang.domain.agent.invoke.AgentSkillInvokeRequest;
import space.ruiwang.domain.agent.invoke.AgentSkillInvokeResponse;
import space.ruiwang.domain.agent.invoke.AgentSkillInvoker;

@Slf4j
public class HttpAgentSkillInvoker implements AgentSkillInvoker {
    private final WebClient.Builder builder;

    public HttpAgentSkillInvoker(WebClient.Builder builder) {
        this.builder = builder;
    }

    @Override
    public AgentSkillInvokeResponse invoke(LlmSkill skill, AgentSkillInvokeRequest request, Duration timeout) {
        if (skill == null || skill.getEndpoint() == null) {
            throw new IllegalArgumentException("Skill endpoint is required");
        }
        AgentEndpoint endpoint = skill.getEndpoint();
        List<String> transports = endpoint.getTransport();
        boolean supportsHttp = transports == null || transports.isEmpty() || supportsTransport(transports, "HTTP");
        if (!supportsHttp) {
            throw new IllegalStateException("Skill does not support HTTP transport: " + skill.getSkillId());
        }
        String url = endpoint.getUrl();
        if (url == null || url.isBlank()) {
            throw new IllegalStateException("HTTP endpoint url is required");
        }
        Duration resolved = timeout != null ? timeout : Duration.ofSeconds(15);
        return builder.build()
                .post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(AgentSkillInvokeResponse.class)
                .block(resolved);
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
}
