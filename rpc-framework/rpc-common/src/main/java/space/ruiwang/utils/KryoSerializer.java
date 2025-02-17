package space.ruiwang.utils;

import java.io.ByteArrayOutputStream;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.serializers.DefaultSerializers;

import space.ruiwang.domain.RpcRequestDO;
import space.ruiwang.domain.RpcResponseDO;

/**
 * Kryo 序列化工具
 * @author wangrui <wangrui45@kuaishou.com>
 * Created on 2025-02-12
 */
public class KryoSerializer {

    private static final Kryo KRYO = new Kryo();

    static {
        // 注册相关的类，建议注册所有可能出现的类以提高性能和兼容性
        KRYO.register(RpcRequestDO.class);
        KRYO.register(RpcResponseDO.class);
        KRYO.register(String.class);
        KRYO.register(Object[].class);
        KRYO.register(Class[].class);
        // 为 Class 类型注册默认的序列化器
        KRYO.register(Class.class, new DefaultSerializers.ClassSerializer());
    }

    /**
     * 序列化对象为字节数组
     *
     * @param object 要序列化的对象
     * @return 序列化后的字节数组
     */
    public static byte[] serialize(Object object) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        // 创建 Output 对象时可指定初始大小
        Output output = new Output(baos);
        KRYO.writeObject(output, object);
        output.close();
        return baos.toByteArray();
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
        Input input = new Input(bytes);
        T object = KRYO.readObject(input, type);
        input.close();
        return object;
    }
}