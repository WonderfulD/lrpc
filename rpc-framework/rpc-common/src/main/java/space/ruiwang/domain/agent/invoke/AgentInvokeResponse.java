package space.ruiwang.domain.agent.invoke;

import java.io.Serializable;
import java.util.List;

import lombok.Data;

/**
 * High-level A2A invoke response.
 */
@Data
public class AgentInvokeResponse implements Serializable {
    private String traceId;
    private String answer;
    private List<AgentSkillInvokeResult> skillResults;
}
