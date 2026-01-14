package space.ruiwang.agent1;

import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import space.ruiwang.agent.sdk.RpcAgentDiscoveryClient;
import space.ruiwang.domain.agent.LlmSkill;

@Slf4j
@Component
public class Agent1DiscoveryRunner implements CommandLineRunner {
    private final Agent1Properties properties;
    private final RpcAgentDiscoveryClient discoveryClient;

    public Agent1DiscoveryRunner(Agent1Properties properties,
                                 RpcAgentDiscoveryClient discoveryClient) {
        this.properties = properties;
        this.discoveryClient = discoveryClient;
    }

    @Override
    public void run(String... args) {
        if (!properties.isAutoDiscover()) {
            return;
        }
        List<LlmSkill> skills = discoveryClient.discover(
                properties.getQuery(),
                properties.getMaxSkills(),
                properties.getMinScore()
        );
        if (skills.isEmpty()) {
            log.info("Discovery returned no skills");
            return;
        }
        log.info("Discovery returned {} skills", skills.size());
        for (LlmSkill skill : skills) {
            log.info("Skill: {}", skill);
        }
    }
}
