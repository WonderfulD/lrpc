package space.ruiwang.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

@Data
@ConfigurationProperties(prefix = "discovery")
public class DiscoveryProperties {
    private int agentRecallTopK = 200;
    private int skillRecallTopK = 200;
    private int coarseTopK = 200;
    private int fineTopK = 20;
    private int finalTopK = 10;
    private int maxSkillsPerAgent = 3;
    private int minAgentRecall = 5;
    private double agentVectorDistanceThreshold = 0.7;
    private double skillVectorDistanceThreshold = 0.7;
}
