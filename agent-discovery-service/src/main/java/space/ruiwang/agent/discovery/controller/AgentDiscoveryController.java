package space.ruiwang.agent.discovery.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import space.ruiwang.agent.discovery.service.AgentDiscoveryService;
import space.ruiwang.domain.agent.dto.AgentDiscoverRequest;
import space.ruiwang.domain.agent.dto.AgentDiscoverResponse;

@RestController
@RequestMapping("/agent")
public class AgentDiscoveryController {
    private final AgentDiscoveryService discoveryService;

    public AgentDiscoveryController(AgentDiscoveryService discoveryService) {
        this.discoveryService = discoveryService;
    }

    @PostMapping("/discover")
    public AgentDiscoverResponse discover(@RequestBody AgentDiscoverRequest request) {
        return discoveryService.discover(request);
    }
}
