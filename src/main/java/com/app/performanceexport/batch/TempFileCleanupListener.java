package com.app.performanceexport.batch;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
@Component
public class TempFileCleanupListener implements JobExecutionListener {

    @Override
    public void afterJob(JobExecution jobExecution) {
        String filePath = jobExecution.getJobParameters().getString("filePath");
        if (filePath != null) {
            try {
                Files.delete(Path.of(filePath));
                log.info("successfully deleted temp file: {}", filePath);
            } catch (IOException e) {
                log.error("Failed to delete temp file: {}", filePath, e);
            }
        }
    }
}