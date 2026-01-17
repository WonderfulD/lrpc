package space.ruiwang.agent.registration;

public class AgentRegistrationState {
    private volatile String agentId;

    public String getAgentId() {
        return agentId;
    }

    public void setAgentId(String agentId) {
        this.agentId = agentId;
    }
}
