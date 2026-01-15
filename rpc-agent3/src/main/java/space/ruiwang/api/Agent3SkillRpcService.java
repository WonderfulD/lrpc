package space.ruiwang.api;

import space.ruiwang.annotation.RpcAgentService;
import space.ruiwang.domain.agent.invoke.AgentSkillInvokeRequest;
import space.ruiwang.domain.agent.invoke.AgentSkillInvokeResponse;
import space.ruiwang.domain.agent.invoke.AgentSkillService;

@RpcAgentService
public class Agent3SkillRpcService implements AgentSkillService {
    private final Agent3SkillExecutor executor;

    public Agent3SkillRpcService(Agent3SkillExecutor executor) {
        this.executor = executor;
    }

    @Override
    public AgentSkillInvokeResponse invoke(AgentSkillInvokeRequest request) {
        return executor.execute(request);
    }
}
