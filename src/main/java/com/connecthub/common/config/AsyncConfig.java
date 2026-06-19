package com.connecthub.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig {
    @Bean(name = "taskExecutor") // Định nghĩa đúng tên Bean mà @Async đang gọi
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        executor.setCorePoolSize(5);     // Số lượng Thread luôn luôn được giữ ấm sẵn sàng chạy
        executor.setMaxPoolSize(20);     // Số lượng Thread tối đa hệ thống có thể phình ra khi overload
        executor.setQueueCapacity(50);   // Kích thước hàng đợi (nếu vượt quá, các request sau phải đợi)
        executor.setThreadNamePrefix("Connect-Hub-"); // Đặt tên Thread để sau này dễ xem Log debug
        executor.initialize();
        return executor;
    }
}
