package space.ruiwang.agent.client;

import space.ruiwang.agent.invoke.AgentInvokeHandler;
import space.ruiwang.domain.agent.invoke.AgentInvokeRequest;
import space.ruiwang.domain.agent.invoke.AgentInvokeResponse;

public class DefaultAgentInvokeClient implements AgentInvokeClient {
    private final AgentInvokeHandler handler;

    public DefaultAgentInvokeClient(AgentInvokeHandler handler) {
        this.handler = handler;
    }

    @Override
    public AgentInvokeResponse invoke(AgentInvokeRequest request) {
        return handler.invoke(request);
    }
}
