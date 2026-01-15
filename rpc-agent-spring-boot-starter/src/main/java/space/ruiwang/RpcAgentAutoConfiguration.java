package space.ruiwang;

import java.io.IOException;
import java.io.InputStream;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.fasterxml.jackson.databind.ObjectMapper;

import space.ruiwang.agent.invoke.AgentInvokeHandler;
import space.ruiwang.domain.agent.AgentCard;
import space.ruiwang.proxy.ProxyAgent;
import space.ruiwang.domain.agent.invoke.AgentSkillService;
import space.ruiwang.AgentSkillExecutor;
import space.ruiwang.AgentInvokeClient;

@Configuration
@EnableScheduling
@EnableConfigurationProperties({RpcAgentProperties.class, DashScopeProperties.class})
@ConditionalOnProperty(prefix = "lrpc.agent", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RpcAgentAutoConfiguration {

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

    @Bean
    @ConditionalOnMissingBean
    public RpcAgentSkillExecutor rpcAgentSkillExecutor(ProxyAgent proxyAgent,
                                                       org.springframework.web.reactive.function.client.WebClient.Builder builder) {
        return new RpcAgentSkillExecutor(proxyAgent, builder);
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
    public AgentInvokeClient agentInvokeClient(ProxyAgent proxyAgent) {
        return proxyAgent.getAgentInvokeProxy(AgentInvokeClient.class);
    }

    @Bean
    @ConditionalOnMissingBean(AgentSkillService.class)
    public AgentSkillService agentSkillService(ObjectProvider<AgentSkillExecutor> executorProvider) {
        return new DefaultAgentSkillRpcService(executorProvider);
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
