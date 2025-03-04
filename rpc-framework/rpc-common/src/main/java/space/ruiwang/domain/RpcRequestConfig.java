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
public class RpcRequestConfig implements Serializable {
    private String loadBalancerType;
    private long retryCount;
    private long timeout;
    private String tolerant;
}