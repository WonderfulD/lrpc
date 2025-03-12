package space.ruiwang.threadpool;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author wangrui <wangrui45@kuaishou.com>
 * Created on 2025-03-12
 */
@Configuration
public class ThreadPoolFactoryRedis {
    /**
     * 创建定时调度线程池，用于服务清理任务
     */
    @Bean
    public ScheduledExecutorService scheduledExecutor() {
        ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(
                Runtime.getRuntime().availableProcessors(),
                new ThreadFactory() {
                    private final AtomicInteger threadNumber = new AtomicInteger(1);
                    @Override
                    public Thread newThread(Runnable r) {
                        Thread t = new Thread(r, "service-cleaner-" + threadNumber.getAndIncrement());
                        // 设置为非守护线程，确保任务可以完成
                        t.setDaemon(false);
                        return t;
                    }
                }
        );

        // 配置线程池行为
        executor.setRemoveOnCancelPolicy(true);  // 取消任务时从队列中移除
        executor.setKeepAliveTime(60, TimeUnit.SECONDS); // 空闲线程保留时间
        executor.allowCoreThreadTimeOut(true);  // 允许核心线程超时

        return executor;
    }
}
