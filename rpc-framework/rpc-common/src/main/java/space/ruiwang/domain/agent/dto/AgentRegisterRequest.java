package space.ruiwang.domain.agent.dto;

import java.io.Serializable;

import lombok.Data;
import space.ruiwang.domain.agent.AgentCard;

/**
 * Agent registration request.
 */
@Data
public class AgentRegisterRequest implements Serializable {
    private AgentCard agent;
}
