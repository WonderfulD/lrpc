package space.ruiwang.decoder;

import static space.ruiwang.RpcClientNetty.RESPONSE_MAP;

import java.util.concurrent.CompletableFuture;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;
import space.ruiwang.domain.RpcResponseDTO;
import space.ruiwang.utils.serializer.ProtobufSerializer;
/**
 * RPC响应解码器
 * @author wangrui <wangrui45@kuaishou.com>
 * Created on 2025-03-24
 */
@Slf4j
public class RpcResponseDecoder extends SimpleChannelInboundHandler<ByteBuf> {
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) {
        try {
            byte[] bytes = new byte[msg.readableBytes()];
            msg.readBytes(bytes);

            RpcResponseDTO response = ProtobufSerializer.deserializeResponse(bytes);
            String requestId = response.getRequestUUID();

            // 找到对应请求的Future并完成它
            CompletableFuture<RpcResponseDTO> future = RESPONSE_MAP.remove(requestId);
            if (future != null) {
                future.complete(response);
            } else {
                log.warn("收到未知请求ID的响应: {}", requestId);
            }
        } catch (Exception e) {
            log.error("处理RPC响应时发生异常", e);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("RPC客户端通道异常", cause);
        // 不关闭连接，由连接池管理连接状态
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        log.info("RPC连接断开: {}", ctx.channel().remoteAddress());
        // 连接断开时可以触发重连，但这里由业务层处理
    }
}
