package space.ruiwang.domain.agent.dto;

import java.io.Serializable;
import java.util.List;

import lombok.Data;

/**
 * Agent discovery request.
 */
@Data
public class AgentDiscoverRequest implements Serializable {
    private String query;
    private Integer topK;
    private Integer maxAgents;
    private Double minScore;
    private String callerAgentId;
    private List<String> excludeAgentIds;
}
