package space.ruiwang.agent2.api;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.databind.ObjectMapper;

import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import space.ruiwang.agent.dashscope.DashScopeClient;
import space.ruiwang.agent.dashscope.DashScopeMessage;

@RestController
public class Agent2Controller {
    private final DashScopeClient dashScopeClient;
    private final ObjectMapper objectMapper;

    public Agent2Controller(DashScopeClient dashScopeClient, ObjectMapper objectMapper) {
        this.dashScopeClient = dashScopeClient;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/openapi/task")
    public Mono<UserTaskResponse> handleTask(@RequestBody UserTaskRequest request) {
        String query = request == null ? null : request.getQuery();
        if (query == null || query.isBlank()) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "query is required"));
        }
        String systemPrompt = "You are a travel info agent. Provide concise factual travel data and suggestions.";
        String userPrompt = "User request: " + query.trim();
        return chat(systemPrompt, userPrompt)
                .map(answer -> {
                    UserTaskResponse response = new UserTaskResponse();
                    response.setAnswer(answer);
                    return response;
                });
    }

    @PostMapping("/agent")
    public Mono<AgentInvokeResponse> handleSkill(@RequestBody AgentInvokeRequest request) {
        if (request == null || request.getSkillId() == null || request.getSkillId().isBlank()) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "skillId is required"));
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
                return Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown skillId"));
        }
        return chat(systemPrompt, userPrompt)
                .map(result -> {
                    AgentInvokeResponse response = new AgentInvokeResponse();
                    response.setSkillId(skillId);
                    response.setResult(result);
                    return response;
                });
    }

    private Mono<String> chat(String systemPrompt, String userPrompt) {
        return Mono.fromCallable(() -> dashScopeClient.chat(List.of(
                        new DashScopeMessage("system", systemPrompt),
                        new DashScopeMessage("user", userPrompt)
                )))
                .subscribeOn(Schedulers.boundedElastic());
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
