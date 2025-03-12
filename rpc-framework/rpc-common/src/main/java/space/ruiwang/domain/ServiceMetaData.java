package space.ruiwang.domain;

import java.io.Serializable;

import cn.hutool.core.util.IdUtil;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author wangrui <wangrui45@kuaishou.com>
 * Created on 2025-02-11
 */
@Data
@EqualsAndHashCode
public class ServiceMetaData implements Serializable {
    private String uuid;
    private String serviceName;
    private String serviceVersion;
    private String serviceAddr;
    private Integer port;
    private Long endTime;

    /**
     * 注意，此构造方法传入ttl，而非endTime，需手动加上当前毫秒值
     */
    public ServiceMetaData(String serviceName, String serviceVersion, String serviceAddr, Integer port, Long ttl) {
        this.uuid = generateUUID();
        this.serviceName = serviceName;
        this.serviceVersion = serviceVersion;
        this.serviceAddr = serviceAddr;
        this.port = port;
        this.endTime = System.currentTimeMillis() + ttl;
    }

    /**
     * 此构造方法不传入ttl，对象endTime为null
     */
    public ServiceMetaData(String serviceName, String serviceVersion, String serviceAddr, Integer port) {
        this.uuid = generateUUID();
        this.serviceName = serviceName;
        this.serviceVersion = serviceVersion;
        this.serviceAddr = serviceAddr;
        this.port = port;
    }

    private static String generateUUID() {
        return IdUtil.simpleUUID();
    }

    // 使用原参数构建新的ServiceDO
    public static ServiceMetaData generateNewService(ServiceMetaData service) {
        ServiceMetaData newService = new ServiceMetaData(
                service.getServiceName(),
                service.getServiceVersion(),
                service.getServiceAddr(),
                service.getPort()
        );
        newService.setEndTime(service.getEndTime());
        newService.setUuid(generateUUID());
        return newService;
    }
}
