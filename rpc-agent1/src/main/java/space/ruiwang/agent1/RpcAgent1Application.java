package space.ruiwang.agent1;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import space.ruiwang.agent.sdk.EnableRpcAgent;

@SpringBootApplication
@EnableRpcAgent
public class RpcAgent1Application {
    public static void main(String[] args) {
        SpringApplication.run(RpcAgent1Application.class, args);
    }
}
