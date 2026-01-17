package space.ruiwang.domain.agent;

import java.io.Serializable;
import java.util.List;

import lombok.Data;

/**
 * Agent-level invocation endpoint.
 */
@Data
public class AgentEndpoint implements Serializable {
    private String url;
    private List<String> transport;
}
