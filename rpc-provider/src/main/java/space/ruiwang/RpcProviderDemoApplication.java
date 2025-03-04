package space.ruiwang;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import lombok.extern.slf4j.Slf4j;
import space.ruiwang.annotation.EnableProviderRpc;

@SpringBootApplication
@EnableProviderRpc
@Slf4j
public class RpcProviderDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(RpcProviderDemoApplication.class, args);
    }

}
