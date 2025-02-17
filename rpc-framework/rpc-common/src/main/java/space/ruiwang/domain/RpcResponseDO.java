package space.ruiwang.domain;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author wangrui <wangrui45@kuaishou.com>
 * Created on 2025-02-12
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RpcResponseDO implements Serializable {
    private String msg;
    private Object result;
}
