package com.app.performanceexport;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@Slf4j
@SpringBootApplication
public class PerformanceExportApplication {

    @PostConstruct
    public void init() {
        log.info("run with {} threads", Runtime.getRuntime().availableProcessors());
    }
    
    public static void main(String[] args) {
        SpringApplication.run(PerformanceExportApplication.class, args);
    }

}
