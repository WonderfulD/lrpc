package space.ruiwang;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import lombok.extern.slf4j.Slf4j;

@SpringBootApplication
@EnableScheduling
@Slf4j
public class RpcCleanerApplication {

    public static void main(String[] args) {
        SpringApplication.run(RpcCleanerApplication.class, args);
        log.info("\n"
                + "  _        _____    _____     _____             _____   _        ______              _   _   ______   _____  \n"
                + " | |      |  __ \\  |  __ \\   / ____|           / ____| | |      |  ____|     /\\     | \\ | | |  ____| |  __ \\ \n"
                + " | |      | |__) | | |__) | | |       ______  | |      | |      | |__       /  \\    |  \\| | | |__    | |__) |\n"
                + " | |      |  _  /  |  ___/  | |      |______| | |      | |      |  __|     / /\\ \\   | . ` | |  __|   |  _  / \n"
                + " | |____  | | \\ \\  | |      | |____           | |____  | |____  | |____   / ____ \\  | |\\  | | |____  | | \\ \\ \n"
                + " |______| |_|  \\_\\ |_|       \\_____|           \\_____| |______| |______| /_/    \\_\\ |_| \\_| |______| |_|  \\_\\ (SpringBoot)\n"
                + "                                                                                                             \n"
                + "                                                                                                             \n");
    }
}
