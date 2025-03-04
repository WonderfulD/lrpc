package space.ruiwang.spi;

import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import space.ruiwang.consumer.RpcConsumer;

/**
 * @author wangrui <wangrui45@kuaishou.com>
 * Created on 2025-02-25
 */
@Configuration
public class RpcConsumerLoader {
    /**
     * 通过 SPI 机制加载 RpcConsumer 的具体实现（例如 tomcat 模块中的 SimpleHttpClient）
     * @param consumersProvider
     * @return
     */
    @Bean
    public RpcConsumer rpcConsumer(ObjectProvider<List<RpcConsumer>> consumersProvider) {
        List<RpcConsumer> consumers = consumersProvider.getIfAvailable(Collections::emptyList);
        return consumers.stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("No RpcConsumer implementation found"));
    }
}
