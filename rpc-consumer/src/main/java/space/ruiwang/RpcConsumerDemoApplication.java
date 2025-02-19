package space.ruiwang;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import lombok.extern.slf4j.Slf4j;
import space.ruiwang.annotation.EnableConsumerRpc;

@SpringBootApplication
@EnableConsumerRpc
@Slf4j
public class RpcConsumerDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(RpcConsumerDemoApplication.class, args);
        log.info("\n\n\n"
                + "██      ██████  ██████   ██████        ██████  ██████  ███    ██ ███████ ██    ██ ███    ███ ███████ ██████  \n"
                + "██      ██   ██ ██   ██ ██            ██      ██    ██ ████   ██ ██      ██    ██ ████  ████ ██      ██   ██ \n"
                + "██      ██████  ██████  ██      █████ ██      ██    ██ ██ ██  ██ ███████ ██    ██ ██ ████ ██ █████   ██████  \n"
                + "██      ██   ██ ██      ██            ██      ██    ██ ██  ██ ██      ██ ██    ██ ██  ██  ██ ██      ██   ██ \n"
                + "███████ ██   ██ ██       ██████        ██████  ██████  ██   ████ ███████  ██████  ██      ██ ███████ ██   ██ \n\n");
    }

}
