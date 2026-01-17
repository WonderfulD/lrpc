package space.ruiwang.domain.agent.invoke;

/**
 * Local skill executor contract for agent implementations.
 */
public interface AgentSkillExecutor {
    AgentSkillInvokeResponse execute(AgentSkillInvokeRequest request);
}
