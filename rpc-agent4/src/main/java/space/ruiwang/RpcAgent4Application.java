package space.ruiwang;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableRpcAgent
public class RpcAgent4Application {
    public static void main(String[] args) {
        SpringApplication.run(RpcAgent4Application.class, args);
    }
}
