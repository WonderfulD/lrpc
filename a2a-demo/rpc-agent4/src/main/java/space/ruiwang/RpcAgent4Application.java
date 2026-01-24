package space.ruiwang;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import space.ruiwang.agent.annotation.EnableAgent;
import space.ruiwang.annotation.EnableAgentConsumerRpc;
import space.ruiwang.annotation.EnableAgentProviderRpc;

@SpringBootApplication
@EnableAgentConsumerRpc
@EnableAgentProviderRpc
@EnableAgent
public class RpcAgent4Application {
    public static void main(String[] args) {
        SpringApplication.run(RpcAgent4Application.class, args);
    }
}
