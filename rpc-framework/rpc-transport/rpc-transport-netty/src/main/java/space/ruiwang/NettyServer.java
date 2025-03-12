package space.ruiwang;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import lombok.extern.slf4j.Slf4j;
import space.ruiwang.api.transport.RpcProvider;

/**
 * @author wangrui <wangrui45@kuaishou.com>
 * Created on 2025-02-20
 */
@Slf4j
public class NettyServer implements RpcProvider {
    @Override
    public void start(String hostName, int port) {
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel ch) throws Exception {
                            // 在这里添加自定义的Handler，用于处理RPC请求
                            ch.pipeline().addLast(new RpcServerHandler());
                        }
                    })
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true);

            ChannelFuture f = bootstrap.bind(hostName, port).sync();
            log.info("Provider-netty[{}-{}] 启动成功", hostName, port);
            log.info("\n"
                    + "  _        _____    _____     _____            _____    _____     ____   __      __  _____   _____    ______   _____  \n"
                    + " | |      |  __ \\  |  __ \\   / ____|          |  __ \\  |  __ \\   / __ \\  \\ \\    / / |_   _| |  __ \\  |  ____| |  __ \\ \n"
                    + " | |      | |__) | | |__) | | |       ______  | |__) | | |__) | | |  | |  \\ \\  / /    | |   | |  | | | |__    | |__) |\n"
                    + " | |      |  _  /  |  ___/  | |      |______| |  ___/  |  _  /  | |  | |   \\ \\/ /     | |   | |  | | |  __|   |  _  / \n"
                    + " | |____  | | \\ \\  | |      | |____           | |      | | \\ \\  | |__| |    \\  /     _| |_  | |__| | | |____  | | \\ \\ \n"
                    + " |______| |_|  \\_\\ |_|       \\_____|          |_|      |_|  \\_\\  \\____/      \\/     |_____| |_____/  |______| |_|  \\_\\ (Netty)\n"
                    + "                                                                                                                      \n");
            f.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            log.error("Provider-Netty[{}-{}] 启动失败: {}", hostName, port, e.getMessage());
        } finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    }
}
