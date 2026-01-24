package space.ruiwang.agent;

import org.springframework.beans.factory.ObjectProvider;

import space.ruiwang.annotation.RpcAgentService;
import space.ruiwang.domain.agent.invoke.AgentSkillExecutor;
import space.ruiwang.domain.agent.invoke.AgentSkillInvokeRequest;
import space.ruiwang.domain.agent.invoke.AgentSkillInvokeResponse;
import space.ruiwang.domain.agent.invoke.AgentSkillService;

@RpcAgentService
public class DefaultAgentSkillRpcService implements AgentSkillService {
    private final ObjectProvider<AgentSkillExecutor> executorProvider;

    public DefaultAgentSkillRpcService(ObjectProvider<AgentSkillExecutor> executorProvider) {
        this.executorProvider = executorProvider;
    }

    @Override
    public AgentSkillInvokeResponse invoke(AgentSkillInvokeRequest request) {
        AgentSkillExecutor executor = executorProvider.getIfAvailable();
        if (executor == null) {
            throw new IllegalStateException("AgentSkillExecutor bean is required for RPC skill invocation");
        }
        return executor.execute(request);
    }
}
