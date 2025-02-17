package space.ruiwang.domain;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author wangrui <wangrui45@kuaishou.com>
 * Created on 2025-02-11
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RpcRequestDO implements Serializable {
    private String serviceName;
    private String serviceVersion;
    private String methodName;
    private Class<?>[] parameterTypes;
    private Object[] parameters;
}
