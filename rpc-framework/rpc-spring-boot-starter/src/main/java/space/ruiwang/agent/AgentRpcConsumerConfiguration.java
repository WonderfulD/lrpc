package space.ruiwang.agent;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import space.ruiwang.domain.agent.invoke.AgentSkillInvoker;
import space.ruiwang.proxy.ProxyAgent;

@Configuration
public class AgentRpcConsumerConfiguration {

    @Bean(name = "rpcAgentSkillInvoker")
    @ConditionalOnMissingBean(name = "rpcAgentSkillInvoker")
    public AgentSkillInvoker rpcAgentSkillInvoker(ProxyAgent proxyAgent) {
        return new RpcAgentSkillInvoker(proxyAgent);
    }
}
