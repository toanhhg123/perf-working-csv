package com.app.performanceexport;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
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

}