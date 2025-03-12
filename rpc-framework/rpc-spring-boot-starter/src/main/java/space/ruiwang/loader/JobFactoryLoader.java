package space.ruiwang.loader;

import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import space.ruiwang.api.jobfactory.IJobFactory;

/**
 * @author wangrui <wangrui45@kuaishou.com>
 * Created on 2025-03-11
 */
@Configuration
public class JobFactoryLoader {

    /**
     * 注入 IJobFactory 的具体实现
     */
    @Bean
    public IJobFactory jobFactory(ObjectProvider<List<IJobFactory>> consumersProvider) {
        List<IJobFactory> consumers = consumersProvider.getIfAvailable(Collections::emptyList);
        return consumers.stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("No IJobFactory implementation found"));
    }
}
