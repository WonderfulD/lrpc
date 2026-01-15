package space.ruiwang.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

@Data
@ConfigurationProperties(prefix = "registry.health-check")
public class RegistryHealthCheckProperties {
    private boolean enabled = true;
    private int intervalSeconds = 30;
    private int timeoutMs = 2000;
    private int failureThreshold = 3;
    private String healthPath = "/lrpc/agent/health";
}
