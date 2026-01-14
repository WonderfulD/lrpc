package space.ruiwang.agent.registry.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

@Data
@ConfigurationProperties(prefix = "registry.cleanup")
public class RegistryCleanupProperties {
    private boolean enabled = true;
    private int intervalSeconds = 3600;
    private long inactiveTtlSeconds = 604800;
}
