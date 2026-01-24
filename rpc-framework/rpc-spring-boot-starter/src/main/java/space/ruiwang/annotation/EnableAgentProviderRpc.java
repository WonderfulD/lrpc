package space.ruiwang.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Import;

import space.ruiwang.agent.AgentRpcProviderConfiguration;

/**
 * Enable A2A provider support on top of LRPC provider.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@EnableProviderRpc
@Import(AgentRpcProviderConfiguration.class)
public @interface EnableAgentProviderRpc {
}
