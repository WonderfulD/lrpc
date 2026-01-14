package space.ruiwang.agent2;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import space.ruiwang.agent.sdk.EnableRpcAgent;

@SpringBootApplication
@EnableRpcAgent
public class RpcAgent2Application {
    public static void main(String[] args) {
        SpringApplication.run(RpcAgent2Application.class, args);
    }
}
