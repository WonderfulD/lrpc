package space.ruiwang.utils.serializer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;

import space.ruiwang.domain.RpcRequestDTO;
import space.ruiwang.domain.RpcResponseDTO;
import space.ruiwang.protobuf.ParamTypeProto;
import space.ruiwang.protobuf.RpcRequestProto;
import space.ruiwang.protobuf.RpcResponseProto;
/**
 * @author wangrui <wangrui45@kuaishou.com>
 * Created on 2025-03-24
 */
public class ProtobufSerializer {
    private static final Logger log = LoggerFactory.getLogger(ProtobufSerializer.class);

    /**
     * 将RpcRequestDTO序列化为字节数组
     */
    public static byte[] serializeRequest(RpcRequestDTO request) {
        RpcRequestProto.Builder builder = RpcRequestProto.newBuilder()
                .setUuid(request.getUuid())
                .setServiceName(request.getServiceName() != null ? request.getServiceName() : "")
                .setServiceVersion(request.getServiceVersion() != null ? request.getServiceVersion() : "")
                .setMethodName(request.getMethodName() != null ? request.getMethodName() : "");

        // 处理参数类型
        Class<?>[] parameterTypes = request.getParameterTypes();
        if (parameterTypes != null) {
            for (Class<?> type : parameterTypes) {
                ParamTypeProto paramType = ParamTypeProto.newBuilder()
                        .setClassName(type.getName())
                        .build();
                builder.addParameterTypes(paramType);
            }
        }

        // 处理参数值
        Object[] parameters = request.getParameters();
        if (parameters != null) {
            for (Object param : parameters) {
                if (param == null) {
                    builder.addParameters(ByteString.EMPTY);
                } else {
                    try {
                        builder.addParameters(ByteString.copyFrom(serializeObject(param)));
                    } catch (IOException e) {
                        log.error("参数序列化失败: {}", param, e);
                        builder.addParameters(ByteString.EMPTY);
                    }
                }
            }
        }

        return builder.build().toByteArray();
    }

    /**
     * 将字节数组反序列化为RpcRequestDTO
     */
    public static RpcRequestDTO deserializeRequest(byte[] data) throws InvalidProtocolBufferException {
        RpcRequestProto proto = RpcRequestProto.parseFrom(data);

        RpcRequestDTO request = new RpcRequestDTO();
        request.setUuid(proto.getUuid());
        request.setServiceName(proto.getServiceName());
        request.setServiceVersion(proto.getServiceVersion());
        request.setMethodName(proto.getMethodName());

        // 反序列化参数类型
        int paramTypeCount = proto.getParameterTypesCount();
        if (paramTypeCount > 0) {
            Class<?>[] paramTypes = new Class<?>[paramTypeCount];
            for (int i = 0; i < paramTypeCount; i++) {
                ParamTypeProto typeProto = proto.getParameterTypes(i);
                try {
                    paramTypes[i] = Class.forName(typeProto.getClassName());
                } catch (ClassNotFoundException e) {
                    log.error("找不到参数类型: {}", typeProto.getClassName(), e);
                    paramTypes[i] = Object.class; // 降级为Object类型
                }
            }
            request.setParameterTypes(paramTypes);
        }

        // 反序列化参数值
        int paramCount = proto.getParametersCount();
        if (paramCount > 0) {
            Object[] params = new Object[paramCount];
            for (int i = 0; i < paramCount; i++) {
                ByteString paramBytes = proto.getParameters(i);
                if (paramBytes.isEmpty()) {
                    params[i] = null;
                } else {
                    try {
                        params[i] = deserializeObject(paramBytes.toByteArray());
                    } catch (Exception e) {
                        log.error("参数反序列化失败", e);
                        params[i] = null;
                    }
                }
            }
            request.setParameters(params);
        }

        return request;
    }

    /**
     * 将RpcResponseDTO序列化为字节数组
     */
    public static byte[] serializeResponse(RpcResponseDTO response) {
        RpcResponseProto.Builder builder = RpcResponseProto.newBuilder()
                .setUuid(response.getUuid())
                .setRequestUuid(response.getRequestUUID() != null ? response.getRequestUUID() : "")
                .setMsg(response.getMsg() != null ? response.getMsg() : "")
                .setCode(response.getCode());

        // 序列化结果对象
        Object result = response.getResult();
        if (result != null) {
            try {
                builder.setResult(ByteString.copyFrom(serializeObject(result)));
            } catch (IOException e) {
                log.error("响应结果序列化失败", e);
                builder.setResult(ByteString.EMPTY);
            }
        }

        return builder.build().toByteArray();
    }

    /**
     * 将字节数组反序列化为RpcResponseDTO
     */
    public static RpcResponseDTO deserializeResponse(byte[] data) throws InvalidProtocolBufferException {
        RpcResponseProto proto = RpcResponseProto.parseFrom(data);

        RpcResponseDTO response = new RpcResponseDTO();
        response.setUuid(proto.getUuid());
        response.setRequestUUID(proto.getRequestUuid());
        response.setMsg(proto.getMsg());
        response.setCode(proto.getCode());

        // 反序列化结果对象
        if (!proto.getResult().isEmpty()) {
            try {
                Object result = deserializeObject(proto.getResult().toByteArray());
                response.setResult(result);
            } catch (Exception e) {
                log.error("响应结果反序列化失败", e);
                response.setResult(null);
            }
        }

        return response;
    }

    /**
     * 将Java对象序列化为字节数组
     */
    private static byte[] serializeObject(Object obj) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(obj);
            return baos.toByteArray();
        }
    }

    /**
     * 将字节数组反序列化为Java对象
     */
    private static Object deserializeObject(byte[] data) throws IOException, ClassNotFoundException {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(data);
                ObjectInputStream ois = new ObjectInputStream(bais)) {
            return ois.readObject();
        }
    }
}