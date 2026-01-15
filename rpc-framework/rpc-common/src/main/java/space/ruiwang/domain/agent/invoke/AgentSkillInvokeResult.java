package space.ruiwang.domain.agent.invoke;

import java.io.Serializable;

import lombok.Data;

/**
 * Result of a single skill invocation.
 */
@Data
public class AgentSkillInvokeResult implements Serializable {
    private String agentId;
    private String skillId;
    private Double score;
    private String result;
}
