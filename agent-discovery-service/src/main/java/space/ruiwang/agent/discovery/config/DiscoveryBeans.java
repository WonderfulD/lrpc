package space.ruiwang.agent.discovery.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisClientConfig;
import redis.clients.jedis.JedisPooled;
import space.ruiwang.agent.dashscope.DashScopeClient;
import space.ruiwang.agent.dashscope.DashScopeConfig;

@Configuration
public class DiscoveryBeans {

    @Bean
    public JedisPooled jedisPooled(RedisStackProperties properties) {
        JedisClientConfig clientConfig = DefaultJedisClientConfig.builder()
                .user(properties.getUsername())
                .password(properties.getPassword())
                .database(properties.getDatabase())
                .build();
        return new JedisPooled(new HostAndPort(properties.getHost(), properties.getPort()), clientConfig);
    }

    @Bean
    public DashScopeClient dashScopeClient(DashScopeProperties properties) {
        DashScopeConfig config = new DashScopeConfig();
        config.setBaseUrl(properties.getBaseUrl());
        config.setApiKey(properties.getApiKey());
        config.setEmbeddingPath(properties.getEmbeddingPath());
        config.setChatPath(properties.getChatPath());
        config.setEmbeddingModel(properties.getEmbeddingModel());
        config.setChatModel(properties.getChatModel());
        config.setTimeoutSeconds(properties.getTimeoutSeconds());
        return new DashScopeClient(config);
    }
}
