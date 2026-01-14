package space.ruiwang.agent.registry;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

import space.ruiwang.agent.registry.config.DashScopeProperties;
import space.ruiwang.agent.registry.config.RedisStackProperties;
import space.ruiwang.agent.registry.config.RegistryCleanupProperties;
import space.ruiwang.agent.registry.config.RegistryHeartbeatProperties;
import space.ruiwang.agent.registry.config.RegistryHealthCheckProperties;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties({
        RedisStackProperties.class,
        DashScopeProperties.class,
        RegistryHeartbeatProperties.class,
        RegistryHealthCheckProperties.class,
        RegistryCleanupProperties.class
})
public class AgentRegistryApplication {
    public static void main(String[] args) {
        SpringApplication.run(AgentRegistryApplication.class, args);
    }
}
