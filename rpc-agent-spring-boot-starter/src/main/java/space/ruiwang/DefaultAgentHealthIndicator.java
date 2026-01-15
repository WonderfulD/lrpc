package space.ruiwang;

public class DefaultAgentHealthIndicator implements AgentHealthIndicator {
    @Override
    public boolean isHealthy() {
        return true;
    }
}
