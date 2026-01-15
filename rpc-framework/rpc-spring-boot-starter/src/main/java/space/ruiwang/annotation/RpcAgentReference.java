package space.ruiwang.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Agent RPC reference marker for A2A invocation.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@RpcReference
public @interface RpcAgentReference {
}
