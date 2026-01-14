package space.ruiwang.agent3;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import space.ruiwang.agent.sdk.EnableRpcAgent;

@SpringBootApplication
@EnableRpcAgent
public class RpcAgent3Application {
    public static void main(String[] args) {
        SpringApplication.run(RpcAgent3Application.class, args);
    }
}
