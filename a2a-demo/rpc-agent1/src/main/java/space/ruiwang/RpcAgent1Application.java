package space.ruiwang;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import space.ruiwang.agent.annotation.EnableRpcAgent;
import space.ruiwang.annotation.EnableAgentConsumerRpc;
import space.ruiwang.annotation.EnableAgentProviderRpc;

@SpringBootApplication
@EnableAgentConsumerRpc
@EnableAgentProviderRpc
@EnableRpcAgent
public class RpcAgent1Application {
    public static void main(String[] args) {
        SpringApplication.run(RpcAgent1Application.class, args);
    }
}
