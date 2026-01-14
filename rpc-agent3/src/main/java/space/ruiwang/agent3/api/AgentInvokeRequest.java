package space.ruiwang.agent3.api;

import lombok.Data;

@Data
public class AgentInvokeRequest {
    private String skillId;
    private Object input;
    private String query;
}
