package space.ruiwang;

import space.ruiwang.domain.agent.invoke.AgentSkillInvokeRequest;
import space.ruiwang.domain.agent.invoke.AgentSkillInvokeResponse;

/**
 * Local skill executor contract for agent implementations.
 */
public interface AgentSkillExecutor {
    AgentSkillInvokeResponse execute(AgentSkillInvokeRequest request);
}
