package space.ruiwang.agent.dashscope;

import java.io.Serializable;

import lombok.Data;

/**
 * DashScope API configuration.
 */
@Data
public class DashScopeConfig implements Serializable {
    private String baseUrl;
    private String apiKey;
    private String embeddingPath;
    private String chatPath;
    private String embeddingModel;
    private String chatModel;
    private int timeoutSeconds = 30;
}
