package space.ruiwang.agent2;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import space.ruiwang.domain.agent.AgentCapabilities;
import space.ruiwang.domain.agent.AgentCard;
import space.ruiwang.domain.agent.AgentEndpoint;
import space.ruiwang.domain.agent.AgentProvider;
import space.ruiwang.domain.agent.SkillCard;

@Configuration
public class Agent2Config {

    @Bean
    public AgentCard agentCard(Environment environment) {
        AgentCard agent = new AgentCard();
        agent.setName("travel-info");
        agent.setDescription("提供旅行规划的数据能力：景点查询、天气查询、路线规划。");
        agent.setVersion("1.0");
        agent.setDocumentationUrl("https://example.com/agents/travel-info");

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

        SkillCard spot = new SkillCard();
        spot.setId("spot.search");
        spot.setName("spot-search");
        spot.setDescription("查询城市/区域景点信息与评分。");
        spot.setVersion("1.0");
        spot.setTags(List.of("attraction", "poi", "search"));
        spot.setInputModes(List.of("application/json"));
        spot.setOutputModes(List.of("application/json"));
        spot.setExamples(List.of("{\"city\":\"Chengdu\"}"));

        SkillCard weather = new SkillCard();
        weather.setId("weather.query");
        weather.setName("weather-query");
        weather.setDescription("查询天气与 7 天游玩天气预报。");
        weather.setVersion("1.0");
        weather.setTags(List.of("weather", "forecast"));
        weather.setInputModes(List.of("application/json"));
        weather.setOutputModes(List.of("application/json"));
        weather.setExamples(List.of("{\"city\":\"Chengdu\"}"));

        SkillCard route = new SkillCard();
        route.setId("route.plan");
        route.setName("route-plan");
        route.setDescription("计算景点之间的路线与预计耗时。");
        route.setVersion("1.0");
        route.setTags(List.of("route", "navigation", "traffic"));
        route.setInputModes(List.of("application/json"));
        route.setOutputModes(List.of("application/json"));
        route.setExamples(List.of("{\"from\":\"A\",\"to\":\"B\"}"));

        agent.setSkills(List.of(spot, weather, route));
        return agent;
    }

    private String buildEndpointUrl(Environment environment) {
        String port = environment.getProperty("server.port", "18083");
        return "http://localhost:" + port + "/agent";
    }
}
