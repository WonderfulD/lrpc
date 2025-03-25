package space.ruiwang.decoder;

import java.util.List;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import lombok.extern.slf4j.Slf4j;
import space.ruiwang.domain.RpcRequestDTO;
import space.ruiwang.utils.serializer.ProtobufSerializer;
/**
 * RPC请求解码器 - 服务端使用
 * 将字节流解码为RpcRequestDTO对象
 * @author wangrui <wangrui45@kuaishou.com>
 * Created on 2025-03-24
 */
@Slf4j
public class RpcRequestDecoder extends ByteToMessageDecoder {

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        try {
            // 读取所有可读字节
            byte[] bytes = new byte[in.readableBytes()];
            in.readBytes(bytes);

            if (log.isDebugEnabled()) {
                log.debug("解码RPC请求: 收到{}字节数据", bytes.length);
            }

            // 反序列化为RpcRequestDTO对象
            RpcRequestDTO requestDTO = ProtobufSerializer.deserializeRequest(bytes);

            if (requestDTO != null) {
                if (log.isDebugEnabled()) {
                    log.debug("解码RPC请求成功: ID={}, 服务={}.{}",
                            requestDTO.getUuid(),
                            requestDTO.getServiceName(),
                            requestDTO.getMethodName());
                }

                // 将解码后的对象添加到输出列表
                out.add(requestDTO);
            } else {
                log.warn("反序列化RPC请求失败，结果为null");
            }
        } catch (Exception e) {
            log.error("解码RPC请求异常", e);
            throw e;
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("RPC请求解码器异常", cause);
        // 可以选择关闭连接或者让异常继续传播
        ctx.fireExceptionCaught(cause);
    }
}
