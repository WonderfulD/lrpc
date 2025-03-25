package space.ruiwang.utils.serializer;

import java.io.ByteArrayOutputStream;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.serializers.DefaultSerializers;

import space.ruiwang.domain.RpcRequestDTO;
import space.ruiwang.domain.RpcResponseDTO;

/**
 * Kryo 序列化工具
 * @author wangrui <wangrui45@kuaishou.com>
 * Created on 2025-02-12
 */
public class KryoSerializer {

    // 使用 ThreadLocal 确保每个线程有独立的 Kryo 实例
    private static final ThreadLocal<Kryo> KRYO_THREAD_LOCAL = ThreadLocal.withInitial(() -> {
        Kryo kryo = new Kryo();
        // 注册所有需要序列化的类
        kryo.register(RpcRequestDTO.class, 1);
        kryo.register(RpcResponseDTO.class, 2);
        kryo.register(String.class, 3);
        kryo.register(Object.class, 4);
        kryo.register(Object[].class, 5);
        kryo.register(Class[].class, 6);
        kryo.register(Class.class, new DefaultSerializers.ClassSerializer(), 7);
        kryo.register(byte[].class, 8);
        return kryo;
    });

    /**
     * 序列化对象为字节数组
     *
     * @param object 要序列化的对象
     * @return 序列化后的字节数组
     */
    public static byte[] serialize(Object object) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (Output output = new Output(baos)) {
            Kryo kryo = KRYO_THREAD_LOCAL.get();
            kryo.writeObject(output, object);
            output.flush();
            return baos.toByteArray();
        }
    }

    /**
     * 从字节数组反序列化为对象
     *
     * @param bytes 要反序列化的字节数组
     * @param type  目标类型
     * @param <T>   目标类型泛型
     * @return 反序列化后的对象
     */
    public static <T> T deserialize(byte[] bytes, Class<T> type) {
        try (Input input = new Input(bytes)) {
            Kryo kryo = KRYO_THREAD_LOCAL.get();
            return kryo.readObject(input, type);
        }
    }
}