package space.ruiwang.encoder;

import io.netty.handler.codec.LengthFieldPrepender;

/**
 * @author wangrui <wangrui45@kuaishou.com>
 * Created on 2025-03-24
 */
public class MessageFrameEncoder extends LengthFieldPrepender {
    public MessageFrameEncoder() {
        super(4); // 添加4字节长度前缀
    }
}
