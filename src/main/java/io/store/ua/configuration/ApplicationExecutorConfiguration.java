package io.store.ua.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
public class ApplicationExecutorConfiguration {
    @Bean
    public ExecutorService cachedExecutorService() {
        return Executors.newCachedThreadPool();
    }
}
