package space.ruiwang.domain.agent.invoke;

import java.io.Serializable;
import java.util.List;

import lombok.Data;

/**
 * High-level A2A invoke request.
 */
@Data
public class AgentInvokeRequest implements Serializable {
    private String query;
    private Integer maxSkills;
    private Double minScore;
    private String callerAgentId;
    private List<String> excludeAgentIds;
    private String traceId;
    private Long timeoutMs;
}
