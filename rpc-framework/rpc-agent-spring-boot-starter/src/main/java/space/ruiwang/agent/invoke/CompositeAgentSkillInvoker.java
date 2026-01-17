package space.ruiwang.agent.invoke;

import java.time.Duration;
import java.util.List;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;

import space.ruiwang.domain.agent.AgentEndpoint;
import space.ruiwang.domain.agent.LlmSkill;
import space.ruiwang.domain.agent.invoke.AgentSkillInvokeRequest;
import space.ruiwang.domain.agent.invoke.AgentSkillInvokeResponse;
import space.ruiwang.domain.agent.invoke.AgentSkillInvoker;

public class CompositeAgentSkillInvoker implements AgentSkillInvoker {
    private final AgentSkillInvoker httpInvoker;
    private final ObjectProvider<AgentSkillInvoker> rpcInvokerProvider;

    public CompositeAgentSkillInvoker(
            @Qualifier("httpAgentSkillInvoker") AgentSkillInvoker httpInvoker,
            @Qualifier("rpcAgentSkillInvoker") ObjectProvider<AgentSkillInvoker> rpcInvokerProvider) {
        this.httpInvoker = httpInvoker;
        this.rpcInvokerProvider = rpcInvokerProvider;
    }

    @Override
    public AgentSkillInvokeResponse invoke(LlmSkill skill, AgentSkillInvokeRequest request, Duration timeout) {
        if (skill == null) {
            throw new IllegalArgumentException("Skill is required");
        }
        AgentEndpoint endpoint = skill.getEndpoint();
        List<String> transports = endpoint != null ? endpoint.getTransport() : null;
        boolean supportsRpc = supportsTransport(transports, "RPC");
        boolean supportsHttp = transports == null || transports.isEmpty() || supportsTransport(transports, "HTTP");

        AgentSkillInvoker rpcInvoker = rpcInvokerProvider.getIfAvailable();
        if (supportsRpc && rpcInvoker != null) {
            return rpcInvoker.invoke(skill, request, timeout);
        }
        if (supportsHttp) {
            return httpInvoker.invoke(skill, request, timeout);
        }
        throw new IllegalStateException("No supported transport for skill " + skill.getSkillId());
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
