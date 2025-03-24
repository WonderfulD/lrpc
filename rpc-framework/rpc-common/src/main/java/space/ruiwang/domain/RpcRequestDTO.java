package space.ruiwang.domain;

import java.io.Serializable;

import cn.hutool.core.util.IdUtil;
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
public class RpcRequestDTO implements Serializable {
    private String uuid;
    private String serviceName;
    private String serviceVersion;
    private String methodName;
    private Class<?>[] parameterTypes;
    private Object[] parameters;

    public static RpcRequestDTO buildRpcRequestDTO() {
        RpcRequestDTO rpcRequestDTO = new RpcRequestDTO();
        rpcRequestDTO.setUuid(generateUUID());
        return rpcRequestDTO;
    }

    public static RpcRequestDTO buildRpcRequestDTO(String serviceName, String serviceVersion, String methodName,
            Class<?>[] parameterTypes, Object[] parameters) {
        return new RpcRequestDTO(generateUUID(), serviceName, serviceVersion, methodName, parameterTypes, parameters);
    }

    private static String generateUUID() {
        return IdUtil.simpleUUID();
    }
}
