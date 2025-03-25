package space.ruiwang;

import java.lang.reflect.Method;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;
import space.ruiwang.domain.RpcRequestDTO;
import space.ruiwang.domain.RpcResponseDTO;
import space.ruiwang.servicemanager.ServiceRegisterUtil;
import space.ruiwang.utils.RpcServiceKeyBuilder;

/**
 * RPC服务器请求处理器
 * 处理解码后的RpcRequestDTO对象
 *
 * @author wangrui <wangrui45@kuaishou.com>
 * Created on 2025-02-20
 */
@Slf4j
public class RpcServerHandler extends SimpleChannelInboundHandler<RpcRequestDTO> {

    // 业务处理线程池，避免阻塞IO线程
    private final ExecutorService executor = Executors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors() * 2,
            r -> {
                Thread thread = new Thread(r, "rpc-business");
                thread.setDaemon(true);
                return thread;
            });

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RpcRequestDTO request) {
        log.info("收到RPC请求: ID={}, serviceKey={}, 方法={}",
                request.getUuid(),
                RpcServiceKeyBuilder.buildServiceKey(request.getServiceName(), request.getServiceVersion()),
                request.getMethodName());

        // 使用业务线程池处理请求
        executor.execute(() -> {
            // 处理请求并获取响应
            RpcResponseDTO response = processRequest(request);

            // 直接写回响应对象，由RpcResponseEncoder负责序列化
            ctx.writeAndFlush(response).addListener(future -> {
                if (!future.isSuccess()) {
                    log.error("发送RPC响应失败: {}", future.cause().getMessage());
                }
            });
        });
    }

    /**
     * 处理RPC请求并返回响应
     */
    private RpcResponseDTO processRequest(RpcRequestDTO request) {
        try {
            // 获取服务实例
            Object serviceInstance = ServiceRegisterUtil.getService(request.getServiceName());
            if (serviceInstance == null) {
                log.error("服务不存在: {}", request.getServiceName());
                return RpcResponseDTO.error(
                        request.getUuid(),
                        "服务不存在: " + request.getServiceName());
            }

            // 获取和执行方法
            try {
                Method method = serviceInstance.getClass().getMethod(
                        request.getMethodName(),
                        request.getParameterTypes());

                Object result = method.invoke(serviceInstance, request.getParameters());
                log.debug("RPC方法执行成功: {}#{}, 结果类型: {}",
                        request.getServiceName(),
                        request.getMethodName(),
                        result != null ? result.getClass().getSimpleName() : "null");

                return RpcResponseDTO.success(request.getUuid(), result);
            } catch (NoSuchMethodException e) {
                log.error("方法不存在: {}#{}", request.getServiceName(), request.getMethodName());
                return RpcResponseDTO.error(
                        request.getUuid(),
                        "方法不存在: " + request.getMethodName());
            } catch (Exception e) {
                log.error("方法调用失败: {}#{}", request.getServiceName(), request.getMethodName(), e);
                return RpcResponseDTO.error(
                        request.getUuid(),
                        "方法调用失败: " + e.getMessage());
            }
        } catch (Exception e) {
            log.error("处理RPC请求时发生异常", e);
            return RpcResponseDTO.error(
                    request.getUuid(),
                    "服务器内部错误: " + e.getMessage());
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("RPC服务器处理器异常", cause);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        log.info("客户端连接已建立: {}", ctx.channel().remoteAddress());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        log.warn("客户端连接已断开: {}", ctx.channel().remoteAddress());
    }

    /**
     * 释放资源
     */
    public void destroy() {
        executor.shutdown();
    }
}