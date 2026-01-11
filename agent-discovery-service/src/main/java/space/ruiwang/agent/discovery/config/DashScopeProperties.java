package space.ruiwang.agent.discovery.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

@Data
@ConfigurationProperties(prefix = "dashscope")
public class DashScopeProperties {
    private String baseUrl;
    private String apiKey;
    private String embeddingPath;
    private String chatPath;
    private String embeddingModel = "text-embedding-v2";
    private String chatModel = "qwen-plus";
    private int timeoutSeconds = 30;
}
