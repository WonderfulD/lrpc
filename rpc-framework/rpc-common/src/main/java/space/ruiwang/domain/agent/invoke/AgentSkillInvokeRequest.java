package space.ruiwang.domain.agent.invoke;

import java.io.Serializable;

import lombok.Data;

/**
 * Skill invocation request used by HTTP and RPC.
 */
@Data
public class AgentSkillInvokeRequest implements Serializable {
    private String skillId;
    private Object input;
    private String query;
}
