package space.ruiwang.domain.agent;

import java.io.Serializable;
import java.util.List;

import lombok.Data;

/**
 * Agent capability description used for registration and discovery.
 *
 * @author wangrui
 */
@Data
public class AgentCard implements Serializable {
    private String agentId;
    private boolean active;
    private String lastModifiedTime;
    private String name;
    private String description;
    private String version;
    private String documentationUrl;
    private AgentProvider provider;
    private AgentEndpoint endpoint;
    private AgentCapabilities capabilities;
    private List<String> defaultInputModes;
    private List<String> defaultOutputModes;
    private List<SkillCard> skills;
}
