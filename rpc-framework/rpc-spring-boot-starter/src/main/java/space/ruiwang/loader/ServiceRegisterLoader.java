package space.ruiwang.loader;

import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import space.ruiwang.api.serviceregister.sub.ILocalServiceRegister;
import space.ruiwang.api.serviceregister.sub.IRemoteServiceRegister;


/**
 * @author wangrui <wangrui45@kuaishou.com>
 * Created on 2025-02-25
 */
@Configuration
public class ServiceRegisterLoader {
    /**
     * 通过 SPI 机制加载 本地注册中心 的具体实现
     */
    @Bean
    public ILocalServiceRegister localServiceRegister(ObjectProvider<List<ILocalServiceRegister>> provider) {
        List<ILocalServiceRegister> consumers = provider.getIfAvailable(Collections::emptyList);
        return consumers.stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("No LocalServiceRegisterImpl implementation found"));
    }

    /**
     * 通过 SPI 机制加载 远程注册中心 的具体实现
     */
    @Bean
    public IRemoteServiceRegister remoteServiceRegister(ObjectProvider<List<IRemoteServiceRegister>> provider) {
        List<IRemoteServiceRegister> consumers = provider.getIfAvailable(Collections::emptyList);
        return consumers.stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("No RemoteServiceRegisterImpl implementation found"));
    }
}
