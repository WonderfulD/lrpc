package space.ruiwang.domain.agent.dto;

import java.io.Serializable;

import lombok.Data;

/**
 * Agent heartbeat request.
 */
@Data
public class AgentHeartbeatRequest implements Serializable {
    private String agentId;
}
