package space.ruiwang;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

import space.ruiwang.config.DashScopeProperties;
import space.ruiwang.config.RedisStackProperties;
import space.ruiwang.config.RegistryCleanupProperties;
import space.ruiwang.config.RegistryHeartbeatProperties;
import space.ruiwang.config.RegistryHealthCheckProperties;

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
