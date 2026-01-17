package space.ruiwang.domain.agent;

import java.io.Serializable;
import java.util.List;

import lombok.Data;

/**
 * Minimal skill payload returned to LLM for invocation.
 */
@Data
public class LlmSkill implements Serializable {
    private String agentId;
    private String skillId;
    private String name;
    private String description;
    private Double score;
    private List<String> tags;
    private List<String> inputModes;
    private List<String> outputModes;
    private AgentEndpoint endpoint;
    private String agentName;
    private String agentDescription;
    private String agentOrganization;
    private String agentVersion;
}
