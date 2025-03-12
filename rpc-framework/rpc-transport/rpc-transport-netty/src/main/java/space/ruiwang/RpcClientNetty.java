package space.ruiwang;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.springframework.stereotype.Component;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.MessageToByteEncoder;
import space.ruiwang.api.transport.RpcConsumer;
import space.ruiwang.domain.RpcRequestConfig;
import space.ruiwang.domain.RpcRequestDO;
import space.ruiwang.domain.RpcResponseDO;
import space.ruiwang.domain.ServiceInstance;
import space.ruiwang.utils.KryoSerializer;

/**
 * @author wangrui <wangrui45@kuaishou.com>
 * Created on 2025-03-10
 */
@Component
public class RpcClientNetty implements RpcConsumer {
    @Override
    public RpcResponseDO send(ServiceInstance serviceInstance, RpcRequestDO rpcRequestDO, RpcRequestConfig rpcRequestConfig) {
        String hostName = serviceInstance.getHostname();
        int port = serviceInstance.getPort();

        EventLoopGroup group = new NioEventLoopGroup();
        try {
            RpcClientHandler handler = new RpcClientHandler();
            Bootstrap bootstrap = new Bootstrap()
                    .group(group)
                    .channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ch.pipeline()
                                    // 添加编码器，将RpcRequestDO编码为ByteBuf
                                    .addLast(new RpcRequestEncoder())
                                    // 添加解码器，将ByteBuf解码为RpcResponseDO
                                    .addLast(handler);
                        }
                    });

            // 连接到服务端
            ChannelFuture connectFuture = bootstrap.connect(hostName, port).sync();

            // 发送请求
            connectFuture.channel().writeAndFlush(rpcRequestDO);

            // 异步等待响应，设置超时时间
            CompletableFuture<RpcResponseDO> responseFuture = handler.getResponseFuture();
            RpcResponseDO response = responseFuture.get(rpcRequestConfig.getTimeout(), TimeUnit.SECONDS);

            return response;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return RpcResponseDO.error("请求被中断: " + e.getMessage());
        } catch (TimeoutException e) {
            return RpcResponseDO.error("请求超时");
        } catch (Exception e) {
            return RpcResponseDO.error("RPC调用异常: " + e.getMessage());
        } finally {
            group.shutdownGracefully();
        }
    }

    /**
     * 客户端处理器，负责处理服务端响应
     */
    private static class RpcClientHandler extends SimpleChannelInboundHandler<ByteBuf> {

        private final CompletableFuture<RpcResponseDO> responseFuture = new CompletableFuture<>();

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) {
            // 读取响应数据
            byte[] bytes = new byte[msg.readableBytes()];
            msg.readBytes(bytes);
            RpcResponseDO response = KryoSerializer.deserialize(bytes, RpcResponseDO.class);
            responseFuture.complete(response);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            responseFuture.completeExceptionally(cause);
            ctx.close();
        }

        public CompletableFuture<RpcResponseDO> getResponseFuture() {
            return responseFuture;
        }
    }

    /**
     * RpcRequestDO编码器，将RpcRequestDO对象编码为ByteBuf
     */
    private static class RpcRequestEncoder extends MessageToByteEncoder<RpcRequestDO> {
        @Override
        protected void encode(ChannelHandlerContext ctx, RpcRequestDO msg, ByteBuf out) {
            byte[] bytes = KryoSerializer.serialize(msg);
            out.writeBytes(bytes);
        }
    }
}
