package space.ruiwang;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import lombok.extern.slf4j.Slf4j;
import space.ruiwang.agent.dashscope.DashScopeClient;
import space.ruiwang.agent.dashscope.DashScopeMessage;
import space.ruiwang.agent.invoke.AgentInvokeHandler;
import space.ruiwang.domain.agent.LlmSkill;
import space.ruiwang.domain.agent.invoke.AgentInvokeRequest;
import space.ruiwang.domain.agent.invoke.AgentInvokeResponse;
import space.ruiwang.domain.agent.invoke.AgentSkillInvokeResult;

@Slf4j
public class DefaultAgentInvokeHandler implements AgentInvokeHandler {
    private static final int DEFAULT_MAX_SKILLS = 5;
    private static final double DEFAULT_MIN_SCORE = 0.6;
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(15);

    private final RpcAgentDiscoveryClient discoveryClient;
    private final RpcAgentSkillExecutor skillExecutor;
    private final DashScopeClient dashScopeClient;

    public DefaultAgentInvokeHandler(RpcAgentDiscoveryClient discoveryClient,
                                     RpcAgentSkillExecutor skillExecutor,
                                     DashScopeClient dashScopeClient) {
        this.discoveryClient = discoveryClient;
        this.skillExecutor = skillExecutor;
        this.dashScopeClient = dashScopeClient;
    }

    @Override
    public AgentInvokeResponse invoke(AgentInvokeRequest request) {
        if (request == null || request.getQuery() == null || request.getQuery().isBlank()) {
            throw new IllegalArgumentException("query is required");
        }
        String traceId = request.getTraceId();
        if (traceId == null || traceId.isBlank()) {
            traceId = UUID.randomUUID().toString().replace("-", "");
        }
        int maxSkills = request.getMaxSkills() != null ? request.getMaxSkills() : DEFAULT_MAX_SKILLS;
        double minScore = request.getMinScore() != null ? request.getMinScore() : DEFAULT_MIN_SCORE;
        Duration timeout = resolveTimeout(request);

        AgentInvokeRequest discoverRequest = new AgentInvokeRequest();
        discoverRequest.setQuery(request.getQuery());
        discoverRequest.setMaxSkills(maxSkills);
        discoverRequest.setMinScore(minScore);
        discoverRequest.setCallerAgentId(request.getCallerAgentId());
        discoverRequest.setExcludeAgentIds(request.getExcludeAgentIds());
        discoverRequest.setTimeoutMs(request.getTimeoutMs());

        List<LlmSkill> skills = discoveryClient.discover(discoverRequest);
        List<AgentSkillInvokeResult> results =
                skillExecutor.executeParallel(skills, request.getQuery(), timeout);

        String answer = buildAnswer(request.getQuery(), results);
        AgentInvokeResponse response = new AgentInvokeResponse();
        response.setTraceId(traceId);
        response.setAnswer(answer);
        response.setSkillResults(results);
        return response;
    }

    private String buildAnswer(String query, List<AgentSkillInvokeResult> results) {
        String systemPrompt = "You are an orchestration agent. Use tool outputs to answer the user.";
        String userPrompt = buildUserPrompt(query, results);
        return dashScopeClient.chat(List.of(
                new DashScopeMessage("system", systemPrompt),
                new DashScopeMessage("user", userPrompt)
        ));
    }

    private String buildUserPrompt(String query, List<AgentSkillInvokeResult> results) {
        StringBuilder builder = new StringBuilder();
        builder.append("User query: ").append(query.trim()).append("\n");
        if (results == null || results.isEmpty()) {
            builder.append("No tool results available. Answer directly.");
            return builder.toString();
        }
        builder.append("Tool results:\n");
        for (AgentSkillInvokeResult result : results) {
            builder.append("- ").append(result.getSkillId()).append(": ")
                    .append(result.getResult()).append("\n");
        }
        builder.append("Use the tool results to provide a concise final answer.");
        return builder.toString();
    }

    private Duration resolveTimeout(AgentInvokeRequest request) {
        if (request != null && request.getTimeoutMs() != null && request.getTimeoutMs() > 0) {
            return Duration.ofMillis(request.getTimeoutMs());
        }
        return DEFAULT_TIMEOUT;
    }
}
