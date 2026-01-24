package space.ruiwang.agent.autoconfigure;

import java.io.IOException;
import java.io.InputStream;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.fasterxml.jackson.databind.ObjectMapper;

import space.ruiwang.agent.card.AgentCardProvider;
import space.ruiwang.agent.client.AgentInvokeClient;
import space.ruiwang.agent.client.DefaultAgentInvokeClient;
import space.ruiwang.agent.client.RpcAgentClient;
import space.ruiwang.agent.client.RpcAgentDiscoveryClient;
import space.ruiwang.agent.config.DashScopeProperties;
import space.ruiwang.agent.config.RpcAgentProperties;
import space.ruiwang.agent.health.AgentHealthIndicator;
import space.ruiwang.agent.health.DefaultAgentHealthIndicator;
import space.ruiwang.domain.agent.AgentCard;
import space.ruiwang.agent.invoke.AgentInvokeHandler;
import space.ruiwang.agent.invoke.CompositeAgentSkillInvoker;
import space.ruiwang.agent.invoke.DefaultAgentInvokeHandler;
import space.ruiwang.agent.invoke.HttpAgentSkillInvoker;
import space.ruiwang.agent.invoke.RpcAgentSkillExecutor;
import space.ruiwang.agent.registration.AgentRegistrationRunner;
import space.ruiwang.agent.registration.AgentRegistrationState;
import space.ruiwang.domain.agent.invoke.AgentSkillInvoker;

@Configuration
@EnableScheduling
@EnableConfigurationProperties({RpcAgentProperties.class, DashScopeProperties.class})
@ConditionalOnProperty(prefix = "lrpc.agent", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AgentAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public AgentHealthIndicator agentHealthIndicator() {
        return new DefaultAgentHealthIndicator();
    }

    @Bean
    @ConditionalOnMissingBean
    public AgentCardProvider agentCardProvider(ObjectProvider<AgentCard> provider,
                                               ObjectProvider<ObjectMapper> mapperProvider,
                                               ResourceLoader resourceLoader) {
        AgentCard agentCard = provider.getIfAvailable();
        if (agentCard == null) {
            agentCard = loadAgentCardFromResource(mapperProvider, resourceLoader);
        }
        if (agentCard == null) {
            throw new IllegalStateException("AgentCard bean or classpath:agentcard.json is required for rpc-agent-spring-boot-starter");
        }
        final AgentCard resolved = agentCard;
        return () -> resolved;
    }

    @Bean
    @ConditionalOnMissingBean
    public AgentCard agentCard(AgentCardProvider provider) {
        return provider.getAgentCard();
    }

    @Bean
    public RpcAgentClient lrpcAgentClient(RpcAgentProperties properties) {
        return new RpcAgentClient(properties);
    }

    @Bean
    public AgentRegistrationRunner agentRegistrationRunner(RpcAgentProperties properties,
                                                           AgentCardProvider provider,
                                                           RpcAgentClient client,
                                                           AgentRegistrationState state) {
        return new AgentRegistrationRunner(properties, provider, client, state);
    }

    @Bean
    public AgentRegistrationState agentRegistrationState() {
        return new AgentRegistrationState();
    }

    @Bean
    public RpcAgentDiscoveryClient rpcAgentDiscoveryClient(RpcAgentProperties properties,
                                                           AgentRegistrationState state,
                                                           org.springframework.web.reactive.function.client.WebClient.Builder builder) {
        return new RpcAgentDiscoveryClient(properties, builder, state);
    }

    @Bean(name = "httpAgentSkillInvoker")
    @ConditionalOnMissingBean(name = "httpAgentSkillInvoker")
    public AgentSkillInvoker httpAgentSkillInvoker(org.springframework.web.reactive.function.client.WebClient.Builder builder) {
        return new HttpAgentSkillInvoker(builder);
    }

    @Bean
    @Primary
    public AgentSkillInvoker agentSkillInvoker(
            @Qualifier("httpAgentSkillInvoker") AgentSkillInvoker httpInvoker,
            @Qualifier("rpcAgentSkillInvoker") ObjectProvider<AgentSkillInvoker> rpcInvokerProvider) {
        return new CompositeAgentSkillInvoker(httpInvoker, rpcInvokerProvider);
    }

    @Bean
    @ConditionalOnMissingBean
    public RpcAgentSkillExecutor rpcAgentSkillExecutor(AgentSkillInvoker skillInvoker) {
        return new RpcAgentSkillExecutor(skillInvoker);
    }

    @Bean
    @ConditionalOnMissingBean
    public AgentInvokeHandler agentInvokeHandler(RpcAgentDiscoveryClient discoveryClient,
                                                 RpcAgentSkillExecutor skillExecutor,
                                                 space.ruiwang.agent.dashscope.DashScopeClient dashScopeClient) {
        return new DefaultAgentInvokeHandler(discoveryClient, skillExecutor, dashScopeClient);
    }

    @Bean
    @ConditionalOnMissingBean
    public AgentInvokeClient agentInvokeClient(AgentInvokeHandler handler) {
        return new DefaultAgentInvokeClient(handler);
    }

    @Bean
    @ConditionalOnMissingBean
    public space.ruiwang.agent.dashscope.DashScopeClient dashScopeClient(DashScopeProperties properties) {
        space.ruiwang.agent.dashscope.DashScopeConfig config = new space.ruiwang.agent.dashscope.DashScopeConfig();
        config.setApiKey(properties.getApiKey());
        config.setBaseUrl(properties.getBaseUrl());
        config.setEmbeddingPath(properties.getEmbeddingPath());
        config.setChatPath(properties.getChatPath());
        config.setEmbeddingModel(properties.getEmbeddingModel());
        config.setChatModel(properties.getChatModel());
        config.setTimeoutSeconds(properties.getTimeoutSeconds());
        return new space.ruiwang.agent.dashscope.DashScopeClient(config);
    }

    private AgentCard loadAgentCardFromResource(ObjectProvider<ObjectMapper> mapperProvider,
                                                ResourceLoader resourceLoader) {
        Resource resource = resourceLoader.getResource("classpath:agentcard.json");
        if (!resource.exists()) {
            return null;
        }
        ObjectMapper mapper = mapperProvider.getIfAvailable(ObjectMapper::new);
        try (InputStream inputStream = resource.getInputStream()) {
            return mapper.readValue(inputStream, AgentCard.class);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read classpath:agentcard.json", e);
        }
    }
}
