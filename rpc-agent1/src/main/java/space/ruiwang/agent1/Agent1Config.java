package space.ruiwang.agent1;

import java.util.List;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import space.ruiwang.domain.agent.AgentCapabilities;
import space.ruiwang.domain.agent.AgentCard;
import space.ruiwang.domain.agent.AgentEndpoint;
import space.ruiwang.domain.agent.AgentProvider;
import space.ruiwang.domain.agent.SkillCard;

@Configuration
@EnableConfigurationProperties(Agent1Properties.class)
public class Agent1Config {

    @Bean
    public AgentCard agentCard(Environment environment) {
        AgentCard agent = new AgentCard();
        agent.setName("travel-planner");
        agent.setDescription("负责旅行规划：拆解需求、组合景点/天气/路线信息生成行程方案，并调用外部能力补齐信息。");
        agent.setVersion("1.0");
        agent.setDocumentationUrl("https://example.com/agents/travel-planner");

        AgentProvider provider = new AgentProvider();
        provider.setOrganization("lrpc-demo");
        provider.setUrl("https://example.com");
        agent.setProvider(provider);

        AgentEndpoint endpoint = new AgentEndpoint();
        endpoint.setUrl(buildEndpointUrl(environment));
        endpoint.setTransport("HTTP");
        agent.setEndpoint(endpoint);

        AgentCapabilities capabilities = new AgentCapabilities();
        capabilities.setStreaming(false);
        agent.setCapabilities(capabilities);

        agent.setDefaultInputModes(List.of("application/json"));
        agent.setDefaultOutputModes(List.of("application/json"));

        SkillCard plan = new SkillCard();
        plan.setId("travel.plan");
        plan.setName("travel-plan");
        plan.setDescription("根据城市和天数生成行程规划草案。");
        plan.setVersion("1.0");
        plan.setTags(List.of("travel", "itinerary", "planning"));
        plan.setInputModes(List.of("application/json"));
        plan.setOutputModes(List.of("application/json"));
        plan.setExamples(List.of("{\"city\":\"Chengdu\",\"days\":3}"));

        SkillCard refine = new SkillCard();
        refine.setId("travel.refine");
        refine.setName("travel-refine");
        refine.setDescription("根据预算/偏好优化行程安排。");
        refine.setVersion("1.0");
        refine.setTags(List.of("optimize", "budget", "time"));
        refine.setInputModes(List.of("application/json"));
        refine.setOutputModes(List.of("application/json"));
        refine.setExamples(List.of("{\"budget\":\"3000\",\"preference\":\"food\"}"));

        agent.setSkills(List.of(plan, refine));
        return agent;
    }

    private String buildEndpointUrl(Environment environment) {
        String port = environment.getProperty("server.port", "18082");
        return "http://localhost:" + port + "/agent";
    }
}
