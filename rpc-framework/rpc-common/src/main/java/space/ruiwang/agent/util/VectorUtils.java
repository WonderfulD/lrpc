package space.ruiwang.agent.util;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;

import cn.hutool.core.collection.CollUtil;

/**
 * Vector encoding helpers for Redis vector search.
 */
public final class VectorUtils {
    private VectorUtils() {
    }

    public static byte[] toFloat32ByteArray(List<Float> vector) {
        if (CollUtil.isEmpty(vector)) {
            return new byte[0];
        }
        ByteBuffer buffer = ByteBuffer.allocate(vector.size() * Float.BYTES);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        for (Float value : vector) {
            buffer.putFloat(value == null ? 0.0f : value);
        }
        return buffer.array();
    }
}
