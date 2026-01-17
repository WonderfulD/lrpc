package space.ruiwang.agent.health;

public class DefaultAgentHealthIndicator implements AgentHealthIndicator {
    @Override
    public boolean isHealthy() {
        return true;
    }
}
