package space.ruiwang.agent.registration;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.PreDestroy;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Scheduled;

import lombok.extern.slf4j.Slf4j;
import space.ruiwang.agent.card.AgentCardProvider;
import space.ruiwang.agent.client.RpcAgentClient;
import space.ruiwang.agent.config.RpcAgentProperties;
import space.ruiwang.domain.agent.AgentCard;

@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
public class AgentRegistrationRunner implements ApplicationRunner {
    private final RpcAgentProperties properties;
    private final AgentCardProvider cardProvider;
    private final RpcAgentClient client;
    private final AgentRegistrationState state;
    private final AtomicReference<String> agentId = new AtomicReference<>();

    public AgentRegistrationRunner(RpcAgentProperties properties,
                                   AgentCardProvider cardProvider,
                                   RpcAgentClient client,
                                   AgentRegistrationState state) {
        this.properties = properties;
        this.cardProvider = cardProvider;
        this.client = client;
        this.state = state;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!properties.isEnabled()) {
            return;
        }
        AgentCard agent = cardProvider.getAgentCard();
        if (agent == null) {
            throw new IllegalStateException("AgentCardProvider returned null AgentCard");
        }
        AgentCard registered = client.register(agent).block(Duration.ofSeconds(10));
        if (registered == null || registered.getAgentId() == null) {
            throw new IllegalStateException("Agent registration failed: empty response");
        }
        agentId.set(registered.getAgentId());
        state.setAgentId(registered.getAgentId());
        log.info("Agent registered with agentId={}", registered.getAgentId());
        sendHeartbeatOnce();
    }

    @Scheduled(fixedDelayString = "${lrpc.agent.heartbeat-interval-seconds:30}000")
    public void heartbeat() {
        if (!properties.isEnabled()) {
            return;
        }
        sendHeartbeatOnce();
    }

    @PreDestroy
    public void onShutdown() {
        if (!properties.isEnabled()) {
            return;
        }
        String id = agentId.get();
        if (id == null) {
            return;
        }
        try {
            client.deregister(id).block(Duration.ofSeconds(5));
            state.setAgentId(null);
            log.info("Agent deregistered with agentId={}", id);
        } catch (Exception e) {
            log.warn("Agent deregister failed: {}", e.getMessage());
        }
    }

    private void sendHeartbeatOnce() {
        String id = agentId.get();
        if (id == null) {
            return;
        }
        try {
            client.heartbeat(id).block(Duration.ofSeconds(5));
            log.debug("Heartbeat sent for agentId={}", id);
        } catch (Exception e) {
            log.warn("Heartbeat failed: {}", e.getMessage());
        }
    }
}
