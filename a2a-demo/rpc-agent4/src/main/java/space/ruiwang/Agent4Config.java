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
public class Agent4Config {

    @Bean
    public AgentCard agentCard(Environment environment) {
        AgentCard agent = new AgentCard();
        agent.setName("book-recommender");
        agent.setDescription("提供图书推荐与解读能力，涵盖热门图书、分类推荐、梗概与人物。");
        agent.setVersion("1.0");
        agent.setDocumentationUrl("https://example.com/agents/book-recommender");

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

        SkillCard trending = new SkillCard();
        trending.setId("book.trending");
        trending.setName("book-trending");
        trending.setDescription("查询近期热门图书榜单与推荐理由。");
        trending.setVersion("1.0");
        trending.setTags(List.of("book", "trending", "recommend"));
        trending.setInputModes(List.of("application/json"));
        trending.setOutputModes(List.of("application/json"));
        trending.setExamples(List.of("{\"region\":\"CN\",\"limit\":10}"));

        SkillCard recommend = new SkillCard();
        recommend.setId("book.recommend");
        recommend.setName("book-recommend");
        recommend.setDescription("根据传入的类型/主题推荐图书。");
        recommend.setVersion("1.0");
        recommend.setTags(List.of("book", "genre", "recommend"));
        recommend.setInputModes(List.of("application/json"));
        recommend.setOutputModes(List.of("application/json"));
        recommend.setExamples(List.of("{\"genre\":\"sci-fi\",\"count\":5}"));

        SkillCard summary = new SkillCard();
        summary.setId("book.summary");
        summary.setName("book-summary");
        summary.setDescription("给出传入图书的故事梗概与主要人物。");
        summary.setVersion("1.0");
        summary.setTags(List.of("book", "summary", "characters"));
        summary.setInputModes(List.of("application/json"));
        summary.setOutputModes(List.of("application/json"));
        summary.setExamples(List.of("{\"title\":\"The Three-Body Problem\",\"author\":\"Liu Cixin\"}"));

        agent.setSkills(List.of(trending, recommend, summary));
        return agent;
    }

    private String buildEndpointUrl(Environment environment) {
        String port = environment.getProperty("server.port", "18085");
        return "http://localhost:" + port + "/agent";
    }
}
