package space.ruiwang;


import lombok.extern.slf4j.Slf4j;
import space.ruiwang.proxy.ProxyFactory;
import space.ruiwang.service.TestService;

/**
 * @author wangrui <wangrui45@kuaishou.com>
 * Created on 2025-02-12
 */
@Slf4j
public class ConsumerTest {
    public static void main(String[] args) {
        TestService testService = ProxyFactory.getProxy(TestService.class);
        String answer = testService.calc(100, 99);
        System.out.println(answer);
    }
}
