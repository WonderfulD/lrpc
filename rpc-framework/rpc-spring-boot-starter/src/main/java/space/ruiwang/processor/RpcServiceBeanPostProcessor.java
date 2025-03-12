package space.ruiwang.processor;


import static space.ruiwang.threadpool.ThreadPoolFactory.RPC_PROVIDER_START_POOL;
import static space.ruiwang.threadpool.ThreadPoolFactory.SERVICE_RENEWAL_POOL;

import java.util.ServiceLoader;
import java.util.concurrent.TimeUnit;

import javax.annotation.PreDestroy;
import javax.annotation.Resource;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Configuration;

import lombok.extern.slf4j.Slf4j;
import space.ruiwang.annotation.RpcService;
import space.ruiwang.api.jobfactory.IJobFactory;
import space.ruiwang.api.serviceregister.sub.IRemoteServiceRegister;
import space.ruiwang.api.transport.RpcProvider;
import space.ruiwang.domain.ServiceMetaData;
import space.ruiwang.servicemanager.ServiceRegisterUtil;
import space.ruiwang.utils.RpcServiceKeyBuilder;

/**
 * 处理 @RpcService 注解的 BeanPostProcessor
 * @author wangrui <wangrui45@kuaishou.com>
 * Created on 2025-02-11
 */
@Slf4j
@Configuration
public class RpcServiceBeanPostProcessor implements BeanPostProcessor {
    private static final String HOST_NAME = "localhost";
    private static final int PORT = 9002;
    @Autowired
    private IRemoteServiceRegister remoteServiceRegister;
    @Resource
    private IJobFactory jobFactory;

    private ServiceMetaData serviceMetaData;

    private boolean serverStarted = false;

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        Class<?> beanClass = bean.getClass();
        // 检查类是否有 @RpcService 注解
        RpcService rpcServiceAnnotation = beanClass.getAnnotation(RpcService.class);
        if (rpcServiceAnnotation != null) {
            String serviceVersion = rpcServiceAnnotation.serviceVersion();
            Class<?> serviceName = rpcServiceAnnotation.service();
            if (serviceName == void.class) {
                // 如果没有指定服务接口，使用该类实现的第一个接口
                Class<?>[] interfaces = beanClass.getInterfaces();
                if (interfaces.length > 0) {
                    serviceName = interfaces[0];
                } else {
                    throw new IllegalStateException("Service interface not specified and class does not implement any interfaces");
                }
            }
            long ttl = rpcServiceAnnotation.ttl();

            // 构造服务注册对象
            serviceMetaData =
                    new ServiceMetaData(serviceName.getName(), serviceVersion, HOST_NAME, PORT, ttl);

            // 注册服务到远程
            remoteServiceRegister.register(serviceMetaData);

            // 扫描指定包下所有标注了 @RpcService 的服务实现类，并注册到 serviceMap 中
            try {
                ServiceRegisterUtil.serviceImplRegister();
            } catch (Exception e) {
                log.warn("Failed to register service implementation", e);
            }

            // 只启动一次服务器
            if (!serverStarted) {
                serverStarted = true;
                startRpcProvider();
            }

            // 线程池提交服务续约任务
            serviceRenewal(ttl, TimeUnit.MILLISECONDS);
        }
        return bean;
    }

    @PreDestroy
    private void shutdown() {
        remoteServiceRegister.deregister(serviceMetaData);
        String key = RpcServiceKeyBuilder.buildServiceKey(serviceMetaData.getServiceName(),
                serviceMetaData.getServiceVersion());
        log.info("服务下线完成。服务 [{}] 下线信息 [{}]",
                key, serviceMetaData);
    }

    /**
     * 启动
     */
    private void startRpcProvider() {
        // 使用SPI机制加载RpcServer实现
        ServiceLoader<RpcProvider> loader = ServiceLoader.load(RpcProvider.class);
        RpcProvider server = loader.findFirst().orElseThrow(() -> new RuntimeException("No RpcProvider implementation found"));

        RPC_PROVIDER_START_POOL.submit(() -> {
            try {
                // 启动RPC服务器
                server.start(HOST_NAME, PORT);
            } catch (Exception e) {
                log.error("Failed to start RPC Provider", e);
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * 服务续约
     * 心跳机制
     */
    private void serviceRenewal(Long time, TimeUnit timeUnit) {
        SERVICE_RENEWAL_POOL.scheduleAtFixedRate(
                jobFactory.createServiceRenewalJob(serviceMetaData, time, timeUnit),
                time / 2,
                time / 2,
                timeUnit
        );
    }
}
