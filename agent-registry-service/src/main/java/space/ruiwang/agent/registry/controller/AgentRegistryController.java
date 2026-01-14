package space.ruiwang.agent.registry.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import space.ruiwang.agent.registry.service.AgentRegistryService;
import space.ruiwang.domain.agent.AgentCard;
import space.ruiwang.domain.agent.dto.AgentDeregisterRequest;
import space.ruiwang.domain.agent.dto.AgentHeartbeatRequest;

@RestController
@RequestMapping("/lrpc/agent")
public class AgentRegistryController {
    private final AgentRegistryService registryService;

    public AgentRegistryController(AgentRegistryService registryService) {
        this.registryService = registryService;
    }

    @PostMapping("/register")
    public AgentCard register(@RequestBody AgentCard agent) {
        return registryService.register(agent);
    }

    @PostMapping("/heartbeat")
    public void heartbeat(@RequestBody AgentHeartbeatRequest request) {
        registryService.heartbeat(request == null ? null : request.getAgentId());
    }

    @PostMapping("/deregister")
    public void deregister(@RequestBody AgentDeregisterRequest request) {
        registryService.deregister(request == null ? null : request.getAgentId());
    }

    @GetMapping("/{agentId}")
    public AgentCard getAgent(@PathVariable String agentId) {
        return registryService.getAgent(agentId);
    }
}
