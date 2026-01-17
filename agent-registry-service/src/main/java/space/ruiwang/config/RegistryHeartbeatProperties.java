package space.ruiwang.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

@Data
@ConfigurationProperties(prefix = "registry.heartbeat")
public class RegistryHeartbeatProperties {
    private int ttlSeconds = 90;
}
