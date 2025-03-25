package space.ruiwang.utils.serializer;

import java.nio.charset.StandardCharsets;

import cn.hutool.json.JSONUtil;

/**
 * @author wangrui <wangrui45@kuaishou.com>
 * Created on 2025-02-12
 */
public class JsonBinarySerializer {
    // JSON序列化
    public static byte[] serialize(Object object) {
        return JSONUtil.toJsonStr(object).getBytes(StandardCharsets.UTF_8);
    }

    // JSON反序列化
    public static <T> T deserialize(byte[] jsonBytes, Class<T> valueType) {
        String jsonStr = new String(jsonBytes, StandardCharsets.UTF_8);
        return JSONUtil.toBean(jsonStr, valueType);
    }
}
