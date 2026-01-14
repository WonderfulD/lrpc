package space.ruiwang.domain.agent.dto;

import java.io.Serializable;

import lombok.Data;

/**
 * Agent deregister request.
 */
@Data
public class AgentDeregisterRequest implements Serializable {
    private String agentId;
}
