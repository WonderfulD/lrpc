package space.ruiwang.encoder;

/**
 * RPC请求编码器
 * @author wangrui <wangrui45@kuaishou.com>
 * Created on 2025-03-24
 */

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import lombok.extern.slf4j.Slf4j;
import space.ruiwang.domain.RpcRequestDTO;
import space.ruiwang.utils.serializer.ProtobufSerializer;
@Slf4j
public class RpcRequestEncoder extends MessageToByteEncoder<RpcRequestDTO> {
    @Override
    protected void encode(ChannelHandlerContext ctx, RpcRequestDTO requestDTO, ByteBuf out) {
        try {
            byte[] bytes = ProtobufSerializer.serializeRequest(requestDTO);
            out.writeBytes(bytes);
        } catch (Exception e) {
            log.error("序列化RPC请求失败", e);
            throw e;
        }
    }
}
