package space.ruiwang.domain.agent.dto;

import java.io.Serializable;
import java.util.List;

import lombok.Data;
import space.ruiwang.domain.agent.AgentCard;
import space.ruiwang.domain.agent.LlmSkill;

/**
 * Agent discovery response.
 */
@Data
public class AgentDiscoverResponse implements Serializable {
    private List<LlmSkill> skills;
    private List<AgentCard> agents;
}
