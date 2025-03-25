package space.ruiwang;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.annotation.PreDestroy;

import org.springframework.stereotype.Component;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.concurrent.DefaultThreadFactory;
import lombok.extern.slf4j.Slf4j;
import space.ruiwang.api.transport.RpcConsumer;
import space.ruiwang.decoder.MessageFrameDecoder;
import space.ruiwang.decoder.RpcResponseDecoder;
import space.ruiwang.domain.RpcRequestConfig;
import space.ruiwang.domain.RpcRequestDTO;
import space.ruiwang.domain.RpcResponseDTO;
import space.ruiwang.domain.ServiceInstance;
import space.ruiwang.encoder.MessageFrameEncoder;
import space.ruiwang.encoder.RpcRequestEncoder;

/**
 * @author wangrui <wangrui45@kuaishou.com>
 * Created on 2025-03-10
 */
@Component
@Slf4j
public class RpcClientNetty implements RpcConsumer {

    // 连接池：用于服务实例地址到Channel的映射
    private final ConcurrentHashMap<String, ChannelHolder> channelPool = new ConcurrentHashMap<>();

    // 用于所有客户端连接的事件循环组
    private final EventLoopGroup eventLoopGroup = new NioEventLoopGroup(
            Runtime.getRuntime().availableProcessors() * 2,
            new DefaultThreadFactory("rpc-client-worker", true));

    // 处理响应的映射，key是请求ID
    public static final Map<String, CompletableFuture<RpcResponseDTO>> RESPONSE_MAP = new ConcurrentHashMap<>();

    @Override
    public RpcResponseDTO send(ServiceInstance serviceInstance, RpcRequestDTO rpcRequestDTO, RpcRequestConfig rpcRequestConfig) {
        String instanceKey = serviceInstance.getHostname() + ":" + serviceInstance.getPort();
        String requestId = rpcRequestDTO.getUuid();
        try {
            // 获取或创建到服务实例的Channel
            Channel channel = getOrCreateChannel(serviceInstance);
            if (channel == null || !channel.isActive()) {
                return RpcResponseDTO.error(requestId, "无法建立到服务器的连接: " + instanceKey);
            }

            // 创建响应的Future
            CompletableFuture<RpcResponseDTO> responseFuture = new CompletableFuture<>();
            RESPONSE_MAP.put(requestId, responseFuture);

            // 发送请求
            channel.writeAndFlush(rpcRequestDTO).addListener(future -> {
                if (!future.isSuccess()) {
                    responseFuture.completeExceptionally(future.cause());
                    RESPONSE_MAP.remove(requestId);
                    log.error("发送请求失败: {}", future.cause().getMessage());
                }
            });

            // 等待响应，设置超时
            try {
                return responseFuture.get(rpcRequestConfig.getTimeout(), TimeUnit.SECONDS);
            } catch (TimeoutException e) {
                RESPONSE_MAP.remove(requestId);
                return RpcResponseDTO.error(requestId, "请求超时");
            } catch (Exception e) {
                RESPONSE_MAP.remove(requestId);
                log.error("获取RPC响应异常", e);
                return RpcResponseDTO.error(requestId, "RPC调用异常: " + e.getMessage());
            }
        } catch (Exception e) {
            log.error("RPC调用过程发生异常", e);
            return RpcResponseDTO.error(requestId, "RPC调用异常: " + e.getMessage());
        }
    }

    /**
     * 获取或创建到特定服务实例的Channel
     */
    private Channel getOrCreateChannel(ServiceInstance serviceInstance) {
        String key = serviceInstance.getHostname() + ":" + serviceInstance.getPort();

        // 尝试获取已有连接
        ChannelHolder holder = channelPool.computeIfAbsent(key, k -> new ChannelHolder());

        Channel channel = holder.getChannel();
        if (channel != null && channel.isActive()) {
            return channel;
        }

        // 创建新连接
        return holder.createChannel(serviceInstance, eventLoopGroup);
    }

    /**
     * 应用关闭时释放资源
     */
    @PreDestroy
    public void destroy() {
        // 关闭所有连接
        for (ChannelHolder holder : channelPool.values()) {
            holder.close();
        }
        channelPool.clear();

        // 清理响应映射
        RESPONSE_MAP.clear();

        // 优雅关闭事件循环组
        if (!eventLoopGroup.isShutdown()) {
            try {
                eventLoopGroup.shutdownGracefully(100, 3000, TimeUnit.MILLISECONDS).sync();
                log.info("RPC客户端事件循环组已关闭");
            } catch (InterruptedException e) {
                log.error("关闭RPC客户端事件循环组时被中断", e);
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Channel持有者，负责管理和保护Channel的生命周期
     */
    private class ChannelHolder {
        private volatile Channel channel;
        private final Lock lock = new ReentrantLock();

        public Channel getChannel() {
            return channel;
        }

        public Channel createChannel(ServiceInstance serviceInstance, EventLoopGroup group) {
            lock.lock();
            try {
                // 双重检查锁定，避免重复创建
                if (channel != null && channel.isActive()) {
                    return channel;
                }

                // 关闭旧连接
                close();

                // 创建新连接
                Bootstrap bootstrap = new Bootstrap()
                        .group(group)
                        .channel(NioSocketChannel.class)
                        .option(ChannelOption.TCP_NODELAY, true)
                        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 3000)
                        .handler(new ChannelInitializer<SocketChannel>() {
                            @Override
                            protected void initChannel(SocketChannel ch) {
                                ch.pipeline()
                                        .addLast(new MessageFrameDecoder())    // 先解决消息边界问题
                                        .addLast(new MessageFrameEncoder())    // 添加长度编码
                                        .addLast(new RpcRequestEncoder())      // 然后处理协议编码
                                        .addLast(new RpcResponseDecoder());    // 最后处理协议解码
                            }
                        });

                ChannelFuture future = bootstrap.connect(serviceInstance.getHostname(), serviceInstance.getPort());
                future.syncUninterruptibly();

                if (!future.isSuccess()) {
                    log.error("连接服务器失败: {}:{}", serviceInstance.getHostname(), serviceInstance.getPort(), future.cause());
                    return null;
                }

                channel = future.channel();
                return channel;
            } finally {
                lock.unlock();
            }
        }

        public void close() {
            if (channel != null) {
                try {
                    channel.close().syncUninterruptibly();
                } catch (Exception e) {
                    log.warn("关闭Channel时发生异常", e);
                } finally {
                    channel = null;
                }
            }
        }
    }
}