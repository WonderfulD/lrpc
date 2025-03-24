package space.ruiwang;

import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.PreDestroy;

import org.springframework.beans.factory.annotation.Value;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.DefaultThreadFactory;
import lombok.extern.slf4j.Slf4j;
import space.ruiwang.api.transport.RpcProvider;
import space.ruiwang.decoder.MessageFrameDecoder;
import space.ruiwang.decoder.RpcRequestDecoder;
import space.ruiwang.encoder.MessageFrameEncoder;
import space.ruiwang.encoder.RpcResponseEncoder;

/**
 * @author wangrui <wangrui45@kuaishou.com>
 * Created on 2025-02-20
 */
@Slf4j
public class RpcServerNetty implements RpcProvider {
    private final AtomicBoolean started = new AtomicBoolean(false);
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel serverChannel;

    @Value("${rpc.server.boss-threads:1}")
    private int bossThreads = 1;

    @Value("${rpc.server.worker-threads:0}")
    private int workerThreads = 0; // 0表示使用默认值(CPU核心数*2)

    @Value("${rpc.server.backlog:128}")
    private int soBacklog = 128;

    @Value("${rpc.server.keepalive:true}")
    private boolean keepAlive = true;

    @Value("${rpc.server.idle-timeout:60}")
    private int idleTimeout = 60; // 空闲超时时间，单位：秒

    @Override
    public void start(String hostName, int port) {
        // 确保服务器只启动一次
        if (!started.compareAndSet(false, true)) {
            log.warn("RPC服务器已经启动，忽略重复启动请求");
            return;
        }

        try {
            // 创建并配置线程组
            bossGroup = new NioEventLoopGroup(bossThreads,
                    new DefaultThreadFactory("rpc-boss", true));

            workerThreads = workerThreads <= 0 ? Runtime.getRuntime().availableProcessors() * 2 : workerThreads;

            workerGroup = new NioEventLoopGroup(workerThreads,
                    new DefaultThreadFactory("rpc-worker", true));

            // 创建服务器引导程序
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .handler(new LoggingHandler(LogLevel.INFO)) // 为服务器通道添加日志处理
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            // 配置通道处理器
                            ch.pipeline()
                                    // 添加空闲连接检测
                                    .addLast(new IdleStateHandler(0, 0, idleTimeout))
                                    // 消息边界处理
                                    .addLast(new MessageFrameDecoder())
                                    .addLast(new MessageFrameEncoder())
                                    // 协议编解码
                                    .addLast(new RpcRequestDecoder())
                                    .addLast(new RpcResponseEncoder())
                                    // 业务逻辑处理
                                    .addLast(new RpcServerHandler());
                        }
                    })
                    // 服务器配置
                    .option(ChannelOption.SO_BACKLOG, soBacklog)
                    .childOption(ChannelOption.SO_KEEPALIVE, keepAlive)
                    .childOption(ChannelOption.TCP_NODELAY, true);

            // 绑定端口并启动服务器
            ChannelFuture bindFuture = bootstrap.bind(hostName, port);
            log.info("RPC服务器正在启动，正在绑定端口 {}:{}...", hostName, port);

            // 等待绑定完成
            bindFuture.syncUninterruptibly();

            // 保存服务器通道
            serverChannel = bindFuture.channel();

            // 输出启动成功消息
            log.info("Provider-netty[{}-{}] 启动成功", hostName, port);
            log.info("\n"
                    + "  _        _____    _____     _____            _____    _____     ____   __      __  _____   _____    ______   _____  \n"
                    + " | |      |  __ \\  |  __ \\   / ____|          |  __ \\  |  __ \\   / __ \\  \\ \\    / / |_   _| |  __ \\  |  ____| |  __ \\ \n"
                    + " | |      | |__) | | |__) | | |       ______  | |__) | | |__) | | |  | |  \\ \\  / /    | |   | |  | | | |__    | |__) |\n"
                    + " | |      |  _  /  |  ___/  | |      |______| |  ___/  |  _  /  | |  | |   \\ \\/ /     | |   | |  | | |  __|   |  _  / \n"
                    + " | |____  | | \\ \\  | |      | |____           | |      | | \\ \\  | |__| |    \\  /     _| |_  | |__| | | |____  | | \\ \\ \n"
                    + " |______| |_|  \\_\\ |_|       \\_____|          |_|      |_|  \\_\\  \\____/      \\/     |_____| |_____/  |______| |_|  \\_\\ (Netty)\n"
                    + "                                                                                                                      \n");

            // 在单独的线程中等待服务器关闭
            new Thread(() -> {
                try {
                    // 等待服务器通道关闭
                    serverChannel.closeFuture().sync();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.warn("等待服务器关闭被中断", e);
                } finally {
                    // 如果服务器通道关闭，确保资源释放
                    shutdown();
                }
            }, "rpc-server-shutdown-hook").start();

        } catch (Exception e) {
            log.error("Provider-Netty[{}-{}] 启动失败: {}", hostName, port, e.getMessage(), e);
            shutdown(); // 确保资源释放
            throw new RuntimeException("启动RPC服务器失败", e);
        }
    }

    /**
     * 异步启动服务器
     */
    public void startAsync(String hostName, int port) {
        new Thread(() -> {
            try {
                start(hostName, port);
            } catch (Exception e) {
                log.error("异步启动RPC服务器失败", e);
            }
        }, "rpc-server-startup-thread").start();
    }

    /**
     * 关闭服务器并释放资源
     */
    @PreDestroy
    public void shutdown() {
        log.info("正在关闭RPC服务器...");

        // 关闭服务器通道
        if (serverChannel != null) {
            try {
                serverChannel.close().syncUninterruptibly();
                serverChannel = null;
            } catch (Exception e) {
                log.warn("关闭服务器通道时发生异常", e);
            }
        }

        // 关闭线程组
        if (bossGroup != null && !bossGroup.isShutdown()) {
            try {
                bossGroup.shutdownGracefully().syncUninterruptibly();
                log.debug("Boss线程组已关闭");
            } catch (Exception e) {
                log.warn("关闭Boss线程组时发生异常", e);
            } finally {
                bossGroup = null;
            }
        }

        if (workerGroup != null && !workerGroup.isShutdown()) {
            try {
                workerGroup.shutdownGracefully().syncUninterruptibly();
                log.debug("Worker线程组已关闭");
            } catch (Exception e) {
                log.warn("关闭Worker线程组时发生异常", e);
            } finally {
                workerGroup = null;
            }
        }

        // 重置启动状态
        started.set(false);
        log.info("RPC服务器已成功关闭");
    }

    /**
     * 检查服务器是否正在运行
     */
    public boolean isRunning() {
        return started.get() && serverChannel != null && serverChannel.isActive();
    }
}
