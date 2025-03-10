package space.ruiwang.impl;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.alibaba.nacos.api.naming.NamingFactory;
import com.alibaba.nacos.api.naming.NamingService;

import lombok.extern.slf4j.Slf4j;

/**
 * @author wangrui <wangrui45@kuaishou.com>
 * Created on 2025-03-10
 */
@Configuration
@Slf4j
public class BeanFactory {
    @Value("localhost:8848")
    private String registryAddr;
    /**
     * 构造方法，传入 nacos 的连接地址，例如：localhost:8848
     */
    @Bean
    public NamingService namingService() {
        try {
            // 创建Nacos命名服务
            return NamingFactory.createNamingService(registryAddr);
        } catch (Exception e) {
            log.error("An error occurred while starting the nacos registry: ", e);
            throw new RuntimeException(e);
        }
    }
}
