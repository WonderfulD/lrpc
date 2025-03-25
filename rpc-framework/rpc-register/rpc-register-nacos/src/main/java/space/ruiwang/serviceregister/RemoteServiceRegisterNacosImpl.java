package space.ruiwang.serviceregister;

import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.annotation.PreDestroy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.listener.NamingEvent;
import com.alibaba.nacos.api.naming.pojo.Instance;

import cn.hutool.core.collection.CollUtil;
import lombok.extern.slf4j.Slf4j;
import space.ruiwang.api.serviceregister.sub.IRemoteServiceRegister;
import space.ruiwang.domain.ServiceMetaData;
import space.ruiwang.utils.RpcServiceKeyBuilder;
import space.ruiwang.utils.ServiceUtil;

/**
 * @author wangrui <wangrui45@kuaishou.com>
 * Created on 2025-03-10
 */
@Component
@Slf4j
@Lazy
public class RemoteServiceRegisterNacosImpl implements IRemoteServiceRegister {

    /**
     * Nacos 命名服务
     */
    @Autowired
    private NamingService namingService;

    @Autowired
    private LocalServiceRegisterNacosImpl localServiceRegister;

    @Override
    public boolean register(ServiceMetaData service) {
        String serviceKey = RpcServiceKeyBuilder.buildServiceKey(service);
        try {
            // 创建服务实例
            Instance instance = new Instance();
            instance.setServiceName(serviceKey);
            instance.setIp(service.getServiceAddr());
            instance.setPort(service.getPort());
            instance.setHealthy(true); // 服务是否健康，和服务发现有关，默认为 true
            instance.setMetadata(ServiceUtil.toMap(service));

            // 注册实例
            namingService.registerInstance(serviceKey, instance);

            log.info("Nacos注册服务成功 [{}].", instance.getServiceName());
            return true;
        } catch (Exception e) {
            throw new RuntimeException(String.format("An error occurred when rpc server registering [%s] service.",
                    service.getServiceName()), e);
        }
    }


    @Override
    public boolean deregister(ServiceMetaData service) {
        try {
            // 创建服务实例
            Instance instance = new Instance();
            instance.setServiceName(service.getServiceName());
            instance.setIp(service.getServiceAddr());
            instance.setPort(service.getPort());
            instance.setHealthy(true); // 服务是否健康，和服务发现有关，默认为 true
            instance.setMetadata(ServiceUtil.toMap(service));

            namingService.deregisterInstance(instance.getServiceName(), instance);
            log.info("Nacos下线服务成功 [{}].", instance.getServiceName());
            return true;
        } catch (NacosException e) {
            throw new RuntimeException(e);
        }
    }

    public List<ServiceMetaData> search(String serviceKey) {
        try {
            // 如果当前服务列表没有被缓存
            List<ServiceMetaData> cached = localServiceRegister.search(serviceKey);
            if (CollUtil.isEmpty(cached)) {
                // 加入缓存 map 中
                localServiceRegister.loadService(serviceKey);

                // 注册指定服务名称下的监听事件，用来实时更新本地服务缓存列表
                namingService.subscribe(serviceKey, event -> {
                    NamingEvent namingEvent = (NamingEvent) event;
                    log.info("[{}]服务缓存更新，该服务目前可用实例有[{}]个", serviceKey, namingEvent.getInstances().size());

                    // 更新本地服务列表缓存
                    localServiceRegister.loadService(serviceKey);
                });
            }
            return localServiceRegister.search(serviceKey);
        } catch (NacosException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean renew(ServiceMetaData service, Long time, TimeUnit timeUnit) {
        log.info("Nacos实现无需手动renew");
        return true;
    }

    @PreDestroy
    public void destroy() throws Exception {
        namingService.shutDown();
        log.info("Nacos远程注册中心销毁完成");
    }
}
