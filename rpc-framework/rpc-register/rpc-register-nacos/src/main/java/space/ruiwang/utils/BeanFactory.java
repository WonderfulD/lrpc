package space.ruiwang.utils;

import java.util.Properties;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.alibaba.nacos.api.PropertyKeyConst;
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
    @Value("${nacos.server-addr:localhost:8848}")
    private String registryAddr;

    @Value("${nacos.username:nacos}")
    private String username;

    @Value("${nacos.password:nacos}")
    private String password;

    /**
     * 构造方法，传入 nacos 的连接地址，例如：localhost:8848
     */
    @Bean
    public NamingService namingService() {
        try {
            Properties properties = new Properties();
            properties.setProperty(PropertyKeyConst.SERVER_ADDR, registryAddr);
            properties.setProperty(PropertyKeyConst.USERNAME, username);
            properties.setProperty(PropertyKeyConst.PASSWORD, password);
            // 创建Nacos命名服务
            return NamingFactory.createNamingService(properties);
        } catch (Exception e) {
            log.error("An error occurred while starting the nacos registry: ", e);
            throw new RuntimeException(e);
        }
    }
}
