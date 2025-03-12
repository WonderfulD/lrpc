package space.ruiwang.threadpool;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;

/**
 * @author wangrui <wangrui45@kuaishou.com>
 * Created on 2025-03-05
 */
public class ThreadPoolFactory {
    /**
     * RPC Server 启动线程池
     */
    public static final ExecutorService RPC_PROVIDER_START_POOL =
            Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r);
                t.setName("rpc-provider-start-thread");
                t.setDaemon(false);
                return t;
            });

    /**
     * 服务实例续约线程池
     */
    public static final ScheduledExecutorService SERVICE_RENEWAL_POOL =
            new ScheduledThreadPoolExecutor(
                    8, // 核心线程数
                    r -> {
                        Thread t = new Thread(r);
                        t.setName("service-servicerenewal-pool-" + t.getId());
                        t.setDaemon(false);
                        return t;
                    }
            );
    /**
     * 过期服务实例删除线程池
     */
    public static final ScheduledExecutorService SERVICE_EXPIRED_REMOVAL_POOL =
            new ScheduledThreadPoolExecutor(
                    8, // 核心线程数
                    r -> {
                        Thread t = new Thread(r);
                        t.setName("service-expired-remove-pool-" + t.getId());
                        t.setDaemon(false);
                        return t;
                    }
            );
}
