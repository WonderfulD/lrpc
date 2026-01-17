package space.ruiwang.domain.agent.invoke;

import java.time.Duration;

import space.ruiwang.domain.agent.LlmSkill;

/**
 * Skill invocation abstraction for agent orchestration.
 */
public interface AgentSkillInvoker {
    AgentSkillInvokeResponse invoke(LlmSkill skill, AgentSkillInvokeRequest request, Duration timeout);
}
