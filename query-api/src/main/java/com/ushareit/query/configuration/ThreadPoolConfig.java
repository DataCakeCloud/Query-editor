package com.ushareit.query.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * @author wuyan
 * @date 2020/5/20
 */
@Configuration
@EnableAsync
public class ThreadPoolConfig {
    /**
     * 默认情况下，在创建了线程池后，线程池中的线程数为0，当有任务来之后，就会创建一个线程去执行任务，
     * 当线程池中的线程数目达到corePoolSize后，就会把到达的任务放到缓存队列当中；
     * 当队列满了，就继续创建线程，当线程数量大于等于maxPoolSize后，开始使用拒绝策略拒绝
     * <p>
     * <p>
     * 注意事项：
     * 如下方式会使@Async失效
     * <p>
     * <p>
     * 一、异步方法使用static修饰
     * 二、异步类没有使用@Component注解（或其他注解）导致spring无法扫描到异步类
     * 三、异步方法不能与被调用的异步方法在同一个类中
     * 四、类中需要使用@Autowired或@Resource等注解自动注入，不能自己手动new对象
     * 五、如果使用SpringBoot框架必须在启动类中增加@EnableAsync注解
     */
    @Bean("taskExecutor")
    public Executor produceThreadPool() {
        ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
        // 设置核心线程数
        taskExecutor.setCorePoolSize(25);
        // 设置最大线程数
        taskExecutor.setMaxPoolSize(50);
        // 设置缓冲队列容量，用来缓冲执行任务的队列
        taskExecutor.setQueueCapacity(500);
        // 设置线程活跃时间（秒）-允许线程的空闲时间60秒：当超过了核心线程出之外的线程在空闲时间到达之后会被销毁
        taskExecutor.setKeepAliveSeconds(60);
        // 设置默认线程名称
        taskExecutor.setThreadNamePrefix("async-service--");
        // 设置拒绝策略
        // rejection-policy：当pool已经达到max size的时候，如何处理新任务
        // CALLER_RUNS：不在新线程中执行任务，而是有调用者所在的线程来执行
        taskExecutor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        // 等待所有任务结束后再关闭线程池
        taskExecutor.setWaitForTasksToCompleteOnShutdown(true);
        taskExecutor.setAwaitTerminationSeconds(60);
        taskExecutor.initialize();
        return taskExecutor;
    }
}