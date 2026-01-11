package space.ruiwang.domain.agent.document;

import java.io.Serializable;
import java.util.List;

import lombok.Data;
import space.ruiwang.domain.agent.AgentEndpoint;

/**
 * Redis document for skill indexing with embedding.
 */
@Data
public class SkillDocument implements Serializable {
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

    private List<Float> embedding;
}
