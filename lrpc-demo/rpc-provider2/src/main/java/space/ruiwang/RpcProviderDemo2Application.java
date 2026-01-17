package space.ruiwang;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import lombok.extern.slf4j.Slf4j;
import space.ruiwang.annotation.EnableProviderRpc;

@SpringBootApplication
@EnableProviderRpc
@Slf4j
public class RpcProviderDemo2Application {

    public static void main(String[] args) {
        SpringApplication.run(RpcProviderDemo2Application.class, args);
    }

}
