package space.ruiwang.agent;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import space.ruiwang.domain.agent.invoke.AgentSkillExecutor;

@Configuration
public class AgentRpcProviderConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public DefaultAgentSkillRpcService defaultAgentSkillRpcService(ObjectProvider<AgentSkillExecutor> executorProvider) {
        return new DefaultAgentSkillRpcService(executorProvider);
    }
}
