package space.ruiwang.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Agent RPC service marker for A2A invocation.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@RpcService
public @interface RpcAgentService {
}
