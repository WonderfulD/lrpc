package space.ruiwang.agent.registry;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import space.ruiwang.agent.registry.config.DashScopeProperties;
import space.ruiwang.agent.registry.config.RedisStackProperties;

@SpringBootApplication
@EnableConfigurationProperties({RedisStackProperties.class, DashScopeProperties.class})
public class AgentRegistryApplication {
    public static void main(String[] args) {
        SpringApplication.run(AgentRegistryApplication.class, args);
    }
}
