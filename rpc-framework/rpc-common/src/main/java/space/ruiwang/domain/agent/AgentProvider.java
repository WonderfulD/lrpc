package space.ruiwang.domain.agent;

import java.io.Serializable;

import lombok.Data;

/**
 * Agent provider metadata.
 */
@Data
public class AgentProvider implements Serializable {
    private String organization;
    private String url;
}
