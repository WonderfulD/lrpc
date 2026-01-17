package space.ruiwang.agent.invoke;

import space.ruiwang.domain.agent.invoke.AgentInvokeRequest;
import space.ruiwang.domain.agent.invoke.AgentInvokeResponse;

/**
 * A2A invocation handler used by @RpcAgentReference.
 */
public interface AgentInvokeHandler {
    AgentInvokeResponse invoke(AgentInvokeRequest request);
}
