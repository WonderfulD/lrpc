package space.ruiwang.domain.agent.document;

import java.util.List;

import lombok.Data;
import lombok.EqualsAndHashCode;
import space.ruiwang.domain.agent.AgentCard;

/**
 * Redis document for agent indexing with embedding.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AgentCardDocument extends AgentCard {
    private List<Float> embedding;
}
