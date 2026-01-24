package space.ruiwang.agent;

import java.time.Duration;
import java.util.List;

import lombok.extern.slf4j.Slf4j;
import space.ruiwang.constants.FaultTolerantStrategies;
import space.ruiwang.constants.LoadBalancerStrategies;
import space.ruiwang.domain.RpcRequestConfig;
import space.ruiwang.domain.agent.AgentEndpoint;
import space.ruiwang.domain.agent.LlmSkill;
import space.ruiwang.domain.agent.invoke.AgentSkillInvokeRequest;
import space.ruiwang.domain.agent.invoke.AgentSkillInvokeResponse;
import space.ruiwang.domain.agent.invoke.AgentSkillInvoker;
import space.ruiwang.domain.agent.invoke.AgentSkillService;
import space.ruiwang.proxy.ProxyAgent;

@Slf4j
public class RpcAgentSkillInvoker implements AgentSkillInvoker {
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(15);

    private final ProxyAgent proxyAgent;

    public RpcAgentSkillInvoker(ProxyAgent proxyAgent) {
        this.proxyAgent = proxyAgent;
    }

    @Override
    public AgentSkillInvokeResponse invoke(LlmSkill skill, AgentSkillInvokeRequest request, Duration timeout) {
        if (skill == null || skill.getEndpoint() == null) {
            throw new IllegalArgumentException("Skill endpoint is required");
        }
        AgentEndpoint endpoint = skill.getEndpoint();
        List<String> transports = endpoint.getTransport();
        boolean supportsRpc = supportsTransport(transports, "RPC");
        if (!supportsRpc) {
            throw new IllegalStateException("Skill does not support RPC transport: " + skill.getSkillId());
        }

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
                resolveTimeout(timeout).toMillis(),
                FaultTolerantStrategies.FAIL_FAST
        );
        AgentSkillService service = proxyAgent.getProxy(AgentSkillService.class, serviceName, version.trim(), config);
        AgentSkillInvokeResponse response = service.invoke(request);
        log.debug("RPC skill invoke completed for {}", skill.getSkillId());
        return response;
    }

    private Duration resolveTimeout(Duration timeout) {
        if (timeout != null) {
            return timeout;
        }
        return DEFAULT_TIMEOUT;
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
