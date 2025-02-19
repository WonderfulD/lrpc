package space.ruiwang.domain;

import java.io.Serializable;

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
public class RpcResponseDO implements Serializable {
    private String msg;
    private Object result;
    private int code;

    public static RpcResponseDO error(String msg) {
        return error(msg, null);
    }

    public static RpcResponseDO error(String msg, Object result) {
        return new RpcResponseDO(msg, result, RpcResponseCode.ERROR);
    }

    public static RpcResponseDO success(Object result) {
        return success("请求成功", result);
    }

    public static RpcResponseDO success(String msg, Object result) {
        return new RpcResponseDO(msg, result, RpcResponseCode.SUCCESS);
    }
}
