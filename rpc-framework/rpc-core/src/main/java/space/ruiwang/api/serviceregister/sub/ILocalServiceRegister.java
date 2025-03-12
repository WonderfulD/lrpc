package space.ruiwang.api.serviceregister.sub;

import space.ruiwang.api.serviceregister.IServiceRegister;

/**
 * @author wangrui <wangrui45@kuaishou.com>
 * Created on 2025-03-10
 */
public interface ILocalServiceRegister extends IServiceRegister {
    boolean loadService(String serviceName, String serviceVersion);
    boolean loadService(String serviceKey);

}
