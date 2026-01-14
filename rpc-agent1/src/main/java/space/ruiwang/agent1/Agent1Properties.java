package space.ruiwang.agent1;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

@Data
@ConfigurationProperties(prefix = "agent1")
public class Agent1Properties {
    private boolean autoDiscover = true;
    private String query = "规划三天旅游行程，优先推荐热门景点";
    private int maxSkills = 10;
    private double minScore = 0.6;
}
