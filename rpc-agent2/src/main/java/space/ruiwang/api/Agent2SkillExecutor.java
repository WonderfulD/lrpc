package space.ruiwang.api;

import java.util.List;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import space.ruiwang.agent.dashscope.DashScopeClient;
import space.ruiwang.agent.dashscope.DashScopeMessage;
import space.ruiwang.domain.agent.invoke.AgentSkillInvokeRequest;
import space.ruiwang.domain.agent.invoke.AgentSkillInvokeResponse;

@Component
public class Agent2SkillExecutor {
    private final DashScopeClient dashScopeClient;
    private final ObjectMapper objectMapper;

    public Agent2SkillExecutor(DashScopeClient dashScopeClient, ObjectMapper objectMapper) {
        this.dashScopeClient = dashScopeClient;
        this.objectMapper = objectMapper;
    }

    public AgentSkillInvokeResponse execute(AgentSkillInvokeRequest request) {
        if (request == null || request.getSkillId() == null || request.getSkillId().isBlank()) {
            throw new IllegalArgumentException("skillId is required");
        }
        String skillId = request.getSkillId().trim();
        String input = formatInput(request.getInput(), request.getQuery());
        String systemPrompt = "You are a travel info agent. Return JSON only.";
        String userPrompt;
        switch (skillId) {
            case "spot.search":
                userPrompt = "Find popular attractions based on input: " + input
                        + ". Return JSON {city,attractions:[{name,rating,highlights}]}";
                break;
            case "weather.query":
                userPrompt = "Provide a 7-day forecast based on input: " + input
                        + ". Return JSON {city,days:[{day,summary,high,low}]}";
                break;
            case "route.plan":
                userPrompt = "Plan a route based on input: " + input
                        + ". Return JSON {from,to,duration,steps:[{mode,detail,duration}]}";
                break;
            default:
                throw new IllegalStateException("Unknown skillId");
        }
        String result = dashScopeClient.chat(List.of(
                new DashScopeMessage("system", systemPrompt),
                new DashScopeMessage("user", userPrompt)
        ));
        AgentSkillInvokeResponse response = new AgentSkillInvokeResponse();
        response.setSkillId(skillId);
        response.setResult(result);
        return response;
    }

    private String formatInput(Object input, String fallback) {
        if (input == null) {
            return fallback == null ? "" : fallback;
        }
        if (input instanceof String) {
            String text = ((String) input).trim();
            return text.isEmpty() ? (fallback == null ? "" : fallback) : text;
        }
        try {
            return objectMapper.writeValueAsString(input);
        } catch (Exception e) {
            return String.valueOf(input);
        }
    }
}
