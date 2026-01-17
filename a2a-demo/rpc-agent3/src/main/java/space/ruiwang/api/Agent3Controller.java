package space.ruiwang.api;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import space.ruiwang.agent.client.AgentInvokeClient;
import space.ruiwang.domain.agent.invoke.AgentInvokeRequest;
import space.ruiwang.domain.agent.invoke.AgentInvokeResponse;
import space.ruiwang.domain.agent.invoke.AgentSkillInvokeRequest;
import space.ruiwang.domain.agent.invoke.AgentSkillInvokeResponse;

@RestController
public class Agent3Controller {
    private final AgentInvokeClient invokeClient;
    private final Agent3SkillExecutor skillExecutor;

    public Agent3Controller(AgentInvokeClient invokeClient, Agent3SkillExecutor skillExecutor) {
        this.invokeClient = invokeClient;
        this.skillExecutor = skillExecutor;
    }

    @PostMapping("/openapi/task")
    public Mono<UserTaskResponse> handleTask(@RequestBody UserTaskRequest request) {
        String query = request == null ? null : request.getQuery();
        if (query == null || query.isBlank()) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "query is required"));
        }
        return Mono.fromCallable(() -> {
                    AgentInvokeRequest invokeRequest = new AgentInvokeRequest();
                    invokeRequest.setQuery(query.trim());
                    invokeRequest.setMaxSkills(6);
                    invokeRequest.setMinScore(0.6);
                    AgentInvokeResponse response = invokeClient.invoke(invokeRequest);
                    UserTaskResponse result = new UserTaskResponse();
                    result.setAnswer(response == null ? "" : response.getAnswer());
                    return result;
                })
                .subscribeOn(Schedulers.boundedElastic());
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

}
