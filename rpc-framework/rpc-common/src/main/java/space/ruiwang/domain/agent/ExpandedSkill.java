package space.ruiwang.domain.agent;

import java.io.Serializable;
import java.util.List;

import lombok.Data;

/**
 * Skill expanded with agent-level fields for routing and ranking.
 */
@Data
public class ExpandedSkill implements Serializable {
    private String agentId;
    private String agentName;
    private String agentDescription;
    private boolean agentActive;
    private AgentEndpoint endpoint;

    private String skillId;
    private String skillName;
    private String skillDescription;
    private List<String> tags;
    private List<String> inputModes;
    private List<String> outputModes;
    private String version;
    private List<String> examples;
}
