package space.ruiwang.api;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import space.ruiwang.agent.dashscope.DashScopeClient;
import space.ruiwang.agent.dashscope.DashScopeMessage;
import space.ruiwang.domain.agent.invoke.AgentSkillInvokeRequest;
import space.ruiwang.domain.agent.invoke.AgentSkillInvokeResponse;

@RestController
public class Agent3Controller {
    private final DashScopeClient dashScopeClient;
    private final Agent3SkillExecutor skillExecutor;

    public Agent3Controller(DashScopeClient dashScopeClient, Agent3SkillExecutor skillExecutor) {
        this.dashScopeClient = dashScopeClient;
        this.skillExecutor = skillExecutor;
    }

    @PostMapping("/openapi/task")
    public Mono<UserTaskResponse> handleTask(@RequestBody UserTaskRequest request) {
        String query = request == null ? null : request.getQuery();
        if (query == null || query.isBlank()) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "query is required"));
        }
        String systemPrompt = "You are an entertainment trends agent. Provide concise trending items.";
        String userPrompt = "User request: " + query.trim();
        return chat(systemPrompt, userPrompt)
                .map(answer -> {
                    UserTaskResponse response = new UserTaskResponse();
                    response.setAnswer(answer);
                    return response;
                });
    }

    @PostMapping("/agent")
    public Mono<AgentSkillInvokeResponse> handleSkill(@RequestBody AgentSkillInvokeRequest request) {
        return Mono.fromCallable(() -> skillExecutor.execute(request))
                .subscribeOn(Schedulers.boundedElastic())
                .onErrorMap(IllegalArgumentException.class,
                        e -> new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage()))
                .onErrorMap(IllegalStateException.class,
                        e -> new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage()));
    }

    private Mono<String> chat(String systemPrompt, String userPrompt) {
        return Mono.fromCallable(() -> dashScopeClient.chat(List.of(
                        new DashScopeMessage("system", systemPrompt),
                        new DashScopeMessage("user", userPrompt)
                )))
                .subscribeOn(Schedulers.boundedElastic());
    }

}
