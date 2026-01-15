package space.ruiwang;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Import;

import space.ruiwang.annotation.EnableAgentConsumerRpc;
import space.ruiwang.annotation.EnableAgentProviderRpc;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import(RpcAgentAutoConfiguration.class)
@EnableAgentConsumerRpc
@EnableAgentProviderRpc
public @interface EnableRpcAgent {
}
