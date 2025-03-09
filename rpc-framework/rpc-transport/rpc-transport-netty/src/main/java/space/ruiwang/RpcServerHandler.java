package space.ruiwang;

import java.lang.reflect.Method;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.extern.slf4j.Slf4j;
import space.ruiwang.domain.RpcRequestDO;
import space.ruiwang.domain.RpcResponseDO;
import space.ruiwang.register.IServiceRegister;
import space.ruiwang.utils.KryoSerializer;
/**
 * @author wangrui <wangrui45@kuaishou.com>
 * Created on 2025-02-20
 */
@Slf4j
public class RpcServerHandler extends ChannelInboundHandlerAdapter {
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        try {
            // 读取序列化后的请求
            ByteBuf byteBuf = (ByteBuf) msg;
            byte[] rpcRequest = new byte[byteBuf.readableBytes()];
            byteBuf.readBytes(rpcRequest);

            // 反序列化请求
            RpcRequestDO rpcRequestDO = KryoSerializer.deserialize(rpcRequest, RpcRequestDO.class);

            // 从rpc请求数据载体获取：服务名、方法名、参数类型列表、参数列表
            String serviceName = rpcRequestDO.getServiceName();
            String methodName = rpcRequestDO.getMethodName();
            Class<?>[] parameterTypes = rpcRequestDO.getParameterTypes();
            Object[] parameters = rpcRequestDO.getParameters();

            // 反射获取方法执行结果
            // 根据注解@RpcService来查找实现了serviceName接口的实现类对象
            Object serviceClass = IServiceRegister.getService(serviceName);
            Method method = serviceClass.getClass().getMethod(methodName, parameterTypes);
            Object result = method.invoke(serviceClass, parameters);

            // 封装为RpcResponseDO
            RpcResponseDO rpcResponseDO = RpcResponseDO.success(result);

            // 序列化结果
            byte[] rpcResponse = KryoSerializer.serialize(rpcResponseDO);

            // 写入结果
            ByteBuf responseBuf = ctx.alloc().buffer(rpcResponse.length);
            responseBuf.writeBytes(rpcResponse);
            ctx.writeAndFlush(responseBuf);
        } catch (Exception e) {
            log.error("执行rpc方法出现异常: {}", e.getMessage());
            // 在出现异常时，可以返回一个错误响应
            RpcResponseDO errorResponse = RpcResponseDO.error(e.getMessage());
            byte[] errorResponseBytes = KryoSerializer.serialize(errorResponse);
            ByteBuf errorResponseBuf = ctx.alloc().buffer(errorResponseBytes.length);
            errorResponseBuf.writeBytes(errorResponseBytes);
            ctx.writeAndFlush(errorResponseBuf);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("RpcServerHandler caught exception", cause);
        ctx.close();
    }
}
