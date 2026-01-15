package space.ruiwang;

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
public class Agent3Config {

    @Bean
    public AgentCard agentCard(Environment environment) {
        AgentCard agent = new AgentCard();
        agent.setName("entertainment-trends");
        agent.setDescription("提供热门音乐/电影榜单查询，与旅行规划无关。");
        agent.setVersion("1.0");
        agent.setDocumentationUrl("https://example.com/agents/entertainment-trends");

        AgentProvider provider = new AgentProvider();
        provider.setOrganization("lrpc-demo");
        provider.setUrl("https://example.com");
        agent.setProvider(provider);

        AgentEndpoint endpoint = new AgentEndpoint();
        endpoint.setUrl(buildEndpointUrl(environment));
        endpoint.setTransport(List.of("HTTP", "RPC"));
        agent.setEndpoint(endpoint);

        AgentCapabilities capabilities = new AgentCapabilities();
        capabilities.setStreaming(false);
        agent.setCapabilities(capabilities);

        agent.setDefaultInputModes(List.of("application/json"));
        agent.setDefaultOutputModes(List.of("application/json"));

        SkillCard music = new SkillCard();
        music.setId("music.trending");
        music.setName("music-trending");
        music.setDescription("查询近期热门音乐榜单。");
        music.setVersion("1.0");
        music.setTags(List.of("music", "trending"));
        music.setInputModes(List.of("application/json"));
        music.setOutputModes(List.of("application/json"));
        music.setExamples(List.of("{\"region\":\"CN\"}"));

        SkillCard movie = new SkillCard();
        movie.setId("movie.trending");
        movie.setName("movie-trending");
        movie.setDescription("查询近期热门电影榜单。");
        movie.setVersion("1.0");
        movie.setTags(List.of("movie", "trending"));
        movie.setInputModes(List.of("application/json"));
        movie.setOutputModes(List.of("application/json"));
        movie.setExamples(List.of("{\"region\":\"CN\"}"));

        agent.setSkills(List.of(music, movie));
        return agent;
    }

    private String buildEndpointUrl(Environment environment) {
        String port = environment.getProperty("server.port", "18084");
        return "http://localhost:" + port + "/agent";
    }
}
