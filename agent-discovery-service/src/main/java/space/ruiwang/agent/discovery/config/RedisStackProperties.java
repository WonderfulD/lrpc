package space.ruiwang.agent.discovery.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

@Data
@ConfigurationProperties(prefix = "redis.stack")
public class RedisStackProperties {
    private String host = "127.0.0.1";
    private int port = 6379;
    private String username;
    private String password;
    private int database = 0;

    private String agentIndex = "agent_idx";
    private String skillIndex = "skill_idx";
    private String agentKeyPrefix = "agent:";
    private String skillKeyPrefix = "skill:";

    private int vectorDimension = 1536;
    private String distanceMetric = "COSINE";
    private int hnswM = 16;
    private int hnswEfConstruction = 200;
}
