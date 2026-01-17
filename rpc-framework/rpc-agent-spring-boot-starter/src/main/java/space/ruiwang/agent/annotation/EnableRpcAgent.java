package space.ruiwang.agent.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Import;

import space.ruiwang.agent.autoconfigure.RpcAgentAutoConfiguration;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import(RpcAgentAutoConfiguration.class)
public @interface EnableRpcAgent {
}
