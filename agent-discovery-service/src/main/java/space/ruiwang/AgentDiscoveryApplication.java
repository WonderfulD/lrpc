package space.ruiwang;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import space.ruiwang.config.DashScopeProperties;
import space.ruiwang.config.DiscoveryProperties;
import space.ruiwang.config.RedisStackProperties;

@SpringBootApplication
@EnableConfigurationProperties({RedisStackProperties.class, DashScopeProperties.class, DiscoveryProperties.class})
public class AgentDiscoveryApplication {
    public static void main(String[] args) {
        SpringApplication.run(AgentDiscoveryApplication.class, args);
    }
}
