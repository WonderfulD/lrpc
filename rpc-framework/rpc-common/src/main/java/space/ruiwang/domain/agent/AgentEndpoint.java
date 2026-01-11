package space.ruiwang.domain.agent;

import java.io.Serializable;

import lombok.Data;

/**
 * Agent-level invocation endpoint.
 */
@Data
public class AgentEndpoint implements Serializable {
    private String url;
    private String transport;
}
