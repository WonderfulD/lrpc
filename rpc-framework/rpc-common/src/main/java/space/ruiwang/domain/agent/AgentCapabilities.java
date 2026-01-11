package space.ruiwang.domain.agent;

import java.io.Serializable;

import lombok.Data;

/**
 * Optional agent capabilities.
 */
@Data
public class AgentCapabilities implements Serializable {
    private boolean streaming;
}
