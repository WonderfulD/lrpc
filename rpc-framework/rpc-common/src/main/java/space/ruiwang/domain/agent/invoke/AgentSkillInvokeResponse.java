package space.ruiwang.domain.agent.invoke;

import java.io.Serializable;

import lombok.Data;

/**
 * Skill invocation response used by HTTP and RPC.
 */
@Data
public class AgentSkillInvokeResponse implements Serializable {
    private String skillId;
    private String result;
}
