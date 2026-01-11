package space.ruiwang.agent.registry.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import space.ruiwang.agent.registry.service.AgentRegistryService;
import space.ruiwang.domain.agent.AgentCard;

@RestController
@RequestMapping("/agent")
public class AgentRegistryController {
    private final AgentRegistryService registryService;

    public AgentRegistryController(AgentRegistryService registryService) {
        this.registryService = registryService;
    }

    @PostMapping("/register")
    public AgentCard register(@RequestBody AgentCard agent) {
        return registryService.register(agent);
    }

    @GetMapping("/{agentId}")
    public AgentCard getAgent(@PathVariable String agentId) {
        return registryService.getAgent(agentId);
    }
}
