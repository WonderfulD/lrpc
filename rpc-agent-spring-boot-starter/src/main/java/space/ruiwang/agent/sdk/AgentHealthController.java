package space.ruiwang.agent.sdk;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Mono;

@RestController
public class AgentHealthController {
    private final AgentHealthIndicator indicator;

    public AgentHealthController(AgentHealthIndicator indicator) {
        this.indicator = indicator;
    }

    @GetMapping("/lrpc/agent/health")
    public Mono<ResponseEntity<Map<String, Object>>> health() {
        boolean healthy = indicator.isHealthy();
        Map<String, Object> body = new HashMap<>();
        body.put("status", healthy ? "UP" : "DOWN");
        if (healthy) {
            return Mono.just(ResponseEntity.ok(body));
        }
        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(body));
    }
}
