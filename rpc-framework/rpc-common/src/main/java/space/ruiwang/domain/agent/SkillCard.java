package space.ruiwang.domain.agent;

import java.io.Serializable;
import java.util.List;

import lombok.Data;

/**
 * Skill description within AgentCard.
 */
@Data
public class SkillCard implements Serializable {
    private String id;
    private String name;
    private String description;
    private String version;
    private List<String> tags;
    private List<String> inputModes;
    private List<String> outputModes;
    private List<String> examples;
}
