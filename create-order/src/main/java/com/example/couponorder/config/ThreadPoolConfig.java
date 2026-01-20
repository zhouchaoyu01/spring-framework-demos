package com.example.couponorder.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * @description <>
 * @author: zhouchaoyu
 * @Date: 2026-01-05
 */
@Configuration
public class ThreadPoolConfig {

    /**
     * 定义一个独立的线程池，用于执行"冲正"、"发通知"等非核心异步任务。
     * 核心目的：舱壁隔离。即使外部优惠券系统挂了导致冲正线程卡死，
     * 也不会耗尽 Tomcat 的主处理线程，保证下单接口能继续响应（虽然会报错）。
     */
    @Bean("compensationExecutor")
    public Executor compensationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // 核心线程数：根据 CPU 核数设置，IO 密集型可稍大
        executor.setCorePoolSize(4);
        // 最大线程数：不要设太大，防止 OOM
        executor.setMaxPoolSize(10);
        // 队列容量：缓冲突发流量
        executor.setQueueCapacity(1000);
        // 线程名称前缀，方便排查日志
        executor.setThreadNamePrefix("async-comp-");
        // 拒绝策略：DiscardPolicy (丢弃)。
        // 为什么丢弃？因为我们有 Redis WAL + 兜底 Job。
        // 如果线程池满了，说明系统负载极高，此时丢弃任务，交给低峰期的 Job 去处理是最好的保护策略。
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardPolicy());
        executor.initialize();
        return executor;
    }
}