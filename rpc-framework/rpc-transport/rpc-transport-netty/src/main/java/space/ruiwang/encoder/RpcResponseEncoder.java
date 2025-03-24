package space.ruiwang.encoder;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import lombok.extern.slf4j.Slf4j;
import space.ruiwang.domain.RpcResponseDTO;
import space.ruiwang.utils.serializer.ProtobufSerializer;
/**
 * RPC响应编码器 - 服务端使用
 * 将RpcResponseDTO对象编码为字节流
 * @author wangrui <wangrui45@kuaishou.com>
 * Created on 2025-03-24
 */
@Slf4j
public class RpcResponseEncoder extends MessageToByteEncoder<RpcResponseDTO> {

    @Override
    protected void encode(ChannelHandlerContext ctx, RpcResponseDTO response, ByteBuf out) throws Exception {
        if (response == null) {
            log.warn("尝试编码null响应");
            return;
        }

        try {
            // 序列化响应对象为字节数组
            byte[] bytes = ProtobufSerializer.serializeResponse(response);

            if (log.isDebugEnabled()) {
                log.debug("编码RPC响应: ID={}, 请求ID={}, 状态码={}, 大小={}字节",
                        response.getUuid(),
                        response.getRequestUUID(),
                        response.getCode(),
                        bytes.length);
            }

            // 写入字节到输出缓冲区
            out.writeBytes(bytes);
        } catch (Exception e) {
            log.error("编码RPC响应异常: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("RPC响应编码器异常", cause);
        ctx.fireExceptionCaught(cause);
    }
}
