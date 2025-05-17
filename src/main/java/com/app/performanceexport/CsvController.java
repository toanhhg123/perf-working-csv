package com.app.performanceexport;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;


@Slf4j
@RestController
@RequiredArgsConstructor
public class CsvController {
    private final CsvService csvService;
    private final CsvGeneratorService csvGeneratorService;
    private final KafkaProtobufProducerService kafkaProtobufProducerService;
    private final TestTimeReadFile testTimeReadFile;

    @PostMapping("/batch")
    public ResponseEntity<Void> insertBatch(@RequestParam("file") MultipartFile file) throws IOException {
        csvService.readLargeCSVWithStream(file);
        return ResponseEntity.ok(null);
    }

    @PostMapping("/kafka")
    public ResponseEntity<Integer> insertKafka(@RequestParam("file") MultipartFile file) throws IOException {
        kafkaProtobufProducerService.processCsvFileAndSendToKafka(file);
        return ResponseEntity.ok(200);
    }

    @PostMapping("/generate")
    public ResponseEntity<Integer> generateCsv() {
        csvGeneratorService.generateFakeUsersCsv();
        return ResponseEntity.ok(200);
    }

    @GetMapping("test-reader-with-spring-batch")
    public ResponseEntity<Void> testReaderWithSpringBatch() throws Exception {
        String filePath = "users_fake.csv";
        testTimeReadFile.readWithSpringBatchReader(filePath);
        return ResponseEntity.ok(null);
    }

    @GetMapping("test-reader-with-univocity")
    public ResponseEntity<Void> testReaderWithUnivocity() throws Exception {
        String filePath = "users_fake.csv";
        testTimeReadFile.readWithUnivocity(filePath);
        return ResponseEntity.ok(null);
    }

}