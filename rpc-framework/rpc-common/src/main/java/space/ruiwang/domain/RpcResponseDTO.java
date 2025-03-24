package space.ruiwang.domain;

import java.io.Serializable;

import cn.hutool.core.util.IdUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import space.ruiwang.constants.RpcResponseCode;

/**
 * @author wangrui <wangrui45@kuaishou.com>
 * Created on 2025-02-12
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RpcResponseDTO implements Serializable {
    private String uuid;
    private String requestUUID;
    private String msg;
    private Object result;
    private int code;

    public static RpcResponseDTO error(String requestUUID) {
        return error(requestUUID, null);
    }
    public static RpcResponseDTO error(String requestUUID, String msg) {
        return error(requestUUID, msg, null);
    }

    public static RpcResponseDTO error(String requestUUID, String msg, Object result) {
        return error(generateUUID(), requestUUID, msg, result, RpcResponseCode.ERROR);
    }

    public static RpcResponseDTO error(String uuid, String requestUUID, String msg, Object result, int code) {
        return new RpcResponseDTO(uuid, requestUUID, msg, result, code);
    }

    public static RpcResponseDTO success(String requestUUID) {
        return success(requestUUID, "请求成功");
    }

    public static RpcResponseDTO success(String requestUUID, String msg) {
        return success(requestUUID, msg, null);
    }

    public static RpcResponseDTO success(String requestUUID, Object result) {
        return success(requestUUID, null, result);
    }

    public static RpcResponseDTO success(String requestUUID, String msg, Object result) {
        return success(generateUUID(), requestUUID, msg, result, RpcResponseCode.SUCCESS);
    }

    public static RpcResponseDTO success(String uuid, String requestUUID, String msg, Object result, int code) {
        return new RpcResponseDTO(uuid, requestUUID, msg, result, code);
    }

    private static String generateUUID() {
        return IdUtil.simpleUUID();
    }
}
