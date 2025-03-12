package space.ruiwang.domain;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author wangrui <wangrui45@kuaishou.com>
 * Created on 2025-02-20
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ServiceInstance implements Serializable {
    private String hostname;
    private int port;

    public ServiceInstance(ServiceMetaData service) {
        this.hostname = service.getServiceAddr();
        this.port = service.getPort();
    }
}
