package space.ruiwang.agent.sdk;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

@Data
@ConfigurationProperties(prefix = "lrpc.agent")
public class RpcAgentProperties {
    private boolean enabled = true;
    private String registryBaseUrl = "http://localhost:18080";
    private String discoveryBaseUrl = "http://localhost:18081";
    private String registerPath = "/lrpc/agent/register";
    private String heartbeatPath = "/lrpc/agent/heartbeat";
    private String deregisterPath = "/lrpc/agent/deregister";
    private String discoverPath = "/agent/discover";
    private int heartbeatIntervalSeconds = 30;
}
