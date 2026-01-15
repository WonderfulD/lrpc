package space.ruiwang;

import space.ruiwang.domain.agent.invoke.AgentInvokeRequest;
import space.ruiwang.domain.agent.invoke.AgentInvokeResponse;

/**
 * A2A invocation entrypoint for discover + execute workflow.
 */
public interface AgentInvokeClient {
    AgentInvokeResponse invoke(AgentInvokeRequest request);
}
