package space.ruiwang.serviceimpl;

import space.ruiwang.references.RpcService;
import space.ruiwang.service.TestService;

/**
 * @author wangrui <wangrui45@kuaishou.com>
 * Created on 2025-02-12
 */
@RpcService(TestService.class)
public class TestServiceImpl implements TestService {
    @Override
    public String calc(Integer a, Integer b) {
        int c = a + b;
        return "a + b 的和是: " + c;
    }
}
