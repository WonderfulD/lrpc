import static space.ruiwang.factory.ThreadPoolFactory.SERVICE_RENEWAL_POOL;

import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import space.ruiwang.RpcProviderDemoApplication;
import space.ruiwang.domain.ServiceRegisterDO;
import space.ruiwang.factory.beanFactory.TaskFactory;
import space.ruiwang.serviceregister.impl.LocalServiceRegister;
import space.ruiwang.utils.RpcServiceKeyBuilder;

/**
 * @author wangrui <wangrui45@kuaishou.com>
 * Created on 2025-03-07
 */
@SpringBootTest(classes = RpcProviderDemoApplication.class)
public class ServiceRenewalTaskTest {

    @Resource
    private TaskFactory taskFactory;
    @Autowired
    private LocalServiceRegister localServiceRegister;

    @DisplayName("测试手动续期")
    @Test
    void run() {
        ServiceRegisterDO serivce =
                new ServiceRegisterDO("space.ruiwang.service.TestService", "1.0", "localhost", 9091);
        serivce.setUuid("8b86397fd2f342ec90532723c661f8d2");
        Long time = 1000L;
        TimeUnit timeUnit = TimeUnit.MILLISECONDS;
        taskFactory.createServiceRenewalTask(serivce, time, timeUnit).run();
        List<ServiceRegisterDO> search = localServiceRegister.search(
                RpcServiceKeyBuilder.buildServiceKey(serivce.getServiceName(), serivce.getServiceVersion()));
        System.out.println(search);
        System.out.println("pause");
    }
}