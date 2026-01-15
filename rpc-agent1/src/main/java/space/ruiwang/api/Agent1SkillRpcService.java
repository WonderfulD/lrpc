package space.ruiwang.api;

import space.ruiwang.annotation.RpcAgentService;
import space.ruiwang.domain.agent.invoke.AgentSkillInvokeRequest;
import space.ruiwang.domain.agent.invoke.AgentSkillInvokeResponse;
import space.ruiwang.domain.agent.invoke.AgentSkillService;

@RpcAgentService
public class Agent1SkillRpcService implements AgentSkillService {
    private final Agent1SkillExecutor executor;

    public Agent1SkillRpcService(Agent1SkillExecutor executor) {
        this.executor = executor;
    }

    @Override
    public AgentSkillInvokeResponse invoke(AgentSkillInvokeRequest request) {
        return executor.execute(request);
    }
}
