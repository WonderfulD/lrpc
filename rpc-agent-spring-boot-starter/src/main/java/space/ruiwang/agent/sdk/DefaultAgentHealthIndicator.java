package space.ruiwang.agent.sdk;

public class DefaultAgentHealthIndicator implements AgentHealthIndicator {
    @Override
    public boolean isHealthy() {
        return true;
    }
}
