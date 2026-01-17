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
        log.info("\n"
                + "  _        _____    _____     _____             _____    ____    _   _    _____   _    _   __  __   ______   _____  \n"
                + " | |      |  __ \\  |  __ \\   / ____|           / ____|  / __ \\  | \\ | |  / ____| | |  | | |  \\/  | |  ____| |  __ \\ \n"
                + " | |      | |__) | | |__) | | |       ______  | |      | |  | | |  \\| | | (___   | |  | | | \\  / | | |__    | |__) |\n"
                + " | |      |  _  /  |  ___/  | |      |______| | |      | |  | | | . ` |  \\___ \\  | |  | | | |\\/| | |  __|   |  _  / \n"
                + " | |____  | | \\ \\  | |      | |____           | |____  | |__| | | |\\  |  ____) | | |__| | | |  | | | |____  | | \\ \\ \n"
                + " |______| |_|  \\_\\ |_|       \\_____|           \\_____|  \\____/  |_| \\_| |_____/   \\____/  |_|  |_| |______| |_|  \\_\\ (SpringBoot)\n"
                + "                                                                                                                    \n"
                + "                                                                                                                    \n");
    }

}
