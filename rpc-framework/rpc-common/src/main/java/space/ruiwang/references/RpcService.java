package space.ruiwang.references;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author wangrui <wangrui45@kuaishou.com>
 * Created on 2025-02-12
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface RpcService {
    Class<?> value();
}
