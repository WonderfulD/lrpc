package space.ruiwang.domain.agent.invoke;

/**
 * RPC entry for agent skill invocation.
 */
public interface AgentSkillService {
    AgentSkillInvokeResponse invoke(AgentSkillInvokeRequest request);
}
