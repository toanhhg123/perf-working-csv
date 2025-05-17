package com.app.performanceexport.batch;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserImportController {
    private final JobLauncher jobLauncher;
    private final Job importUserJob;

    @PostMapping("/import")
    public ResponseEntity<String> handleCsvUpload(@RequestParam("file") MultipartFile file) {
        try {
            String tempDir = System.getProperty("java.io.tmpdir");
            String filename = "upload-" + UUID.randomUUID() + ".csv";
            File tempFile = new File(tempDir, filename);
            file.transferTo(tempFile);

            JobParameters jobParameters = new JobParametersBuilder()
                    .addString("filePath", tempFile.getAbsolutePath())
                    .addLong("startAt", System.currentTimeMillis()) // để runId khác nhau
                    .toJobParameters();

            JobExecution execution = jobLauncher.run(importUserJob, jobParameters);
            return ResponseEntity.ok("Batch job started: ID = " + execution.getId());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Failed to run job: " + e.getMessage());
        }
    }

}
