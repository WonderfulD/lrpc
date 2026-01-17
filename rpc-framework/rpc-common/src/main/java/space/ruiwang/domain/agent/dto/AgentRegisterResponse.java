package space.ruiwang.domain.agent.dto;

import java.io.Serializable;

import lombok.Data;
import space.ruiwang.domain.agent.AgentCard;

/**
 * Agent registration response.
 */
@Data
public class AgentRegisterResponse implements Serializable {
    private AgentCard agent;
}
