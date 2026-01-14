package space.ruiwang.agent.sdk;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

@Data
@ConfigurationProperties(prefix = "dashscope")
public class DashScopeProperties {
    private String baseUrl;
    private String apiKey;
    private String embeddingPath;
    private String chatPath;
    private String embeddingModel;
    private String chatModel;
    private int timeoutSeconds = 30;
}
