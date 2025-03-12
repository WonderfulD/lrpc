package space.ruiwang.serviceimpl;

import space.ruiwang.annotation.RpcService;
import space.ruiwang.service.TestService;

/**
 * @author wangrui <wangrui45@kuaishou.com>
 * Created on 2025-02-12
 */
@RpcService
public class TestServiceImpl implements TestService {
    @Override
    public String calc(Integer a, Integer b) {
        int c = a + b;
        int i = 1 / 0;
        return "a + b 的和是: " + c;
    }
}
