package com.app.performanceexport;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
public class ExecutorConfig {

    @Bean
    public Executor userWriterExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);          // Số luồng hoạt động chính
        executor.setMaxPoolSize(10);          // Số luồng tối đa
        executor.setQueueCapacity(200);       // Hàng đợi task nếu thread chưa sẵn sàng
        executor.setThreadNamePrefix("user-writer-");
        executor.initialize();
        return executor;
    }

    @Bean(name = "csvExecutor")
    public Executor csvExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        int processors = Runtime.getRuntime().availableProcessors();
        executor.setCorePoolSize(processors);
        executor.setMaxPoolSize(processors * 2);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("csv-executor-");
        executor.initialize();
        return executor;
    }
}
