package space.ruiwang.decoder;

import io.netty.handler.codec.LengthFieldBasedFrameDecoder;

/**
 * @author wangrui <wangrui45@kuaishou.com>
 * Created on 2025-03-24
 */
public class MessageFrameDecoder extends LengthFieldBasedFrameDecoder {
    public MessageFrameDecoder() {
        // maxFrameLength, lengthFieldOffset, lengthFieldLength, lengthAdjustment, initialBytesToStrip
        super(10 * 1024 * 1024, 0, 4, 0, 4);
    }
}
