package space.ruiwang.register.sub;

import space.ruiwang.register.IServiceRegister;

/**
 * @author wangrui <wangrui45@kuaishou.com>
 * Created on 2025-03-10
 */
public interface ILocalServiceRegister extends IServiceRegister {
    boolean loadService(String serviceName, String serviceVersion);
    boolean loadService(String serviceKey);

}
