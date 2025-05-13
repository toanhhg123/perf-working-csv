package com.app.performanceexport;

import com.univocity.parsers.csv.CsvParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.app.performanceexport.CsvService.getCsvParser;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaProtobufProducerService {
    private final KafkaTemplate<String, byte[]> kafkaTemplate;
    private static final int BATCH_SIZE = 10_000;

    @Value("${app.kafka.user-topic}")
    private String userTopic;


    public void processCsvFileAndSendToKafka(MultipartFile file) throws IOException {
        log.info("📥 Start processing CSV file: {}", file.getOriginalFilename());
        long start = System.currentTimeMillis();
        CsvParser parser = getCsvParser(file);

        List<UserProto.User> batch = new ArrayList<>(BATCH_SIZE);
        int total = 0;
        int malformed = 0;
        String[] row;

        while ((row = parser.parseNext()) != null) {
            UserProto.User user = mapRowToProto(row);
            if (user == null) {
                malformed++;
                continue;
            }

            batch.add(user);
            if (batch.size() >= BATCH_SIZE) {
                sendBatchToKafka(
                        String.format("batch_%05d_%05d", total, total + batch.size()),
                        batch
                );
                total += batch.size();
                batch.clear();
            }
        }

        if (!batch.isEmpty()) {
            sendBatchToKafka(
                    String.format("batch_%05d_%05d", total, total + batch.size()),
                    batch
            );
            total += batch.size();
        }


        long duration = System.currentTimeMillis() - start;
        log.info("✅ DONE. Sent {} users in {} ms (~{} sec). Skipped {} malformed rows.",
                total, duration, duration / 1000, malformed);
    }

    private void sendBatchToKafka(String batchId, List<UserProto.User> batchUsers) {
        UserProto.UserBatch batch = UserProto.UserBatch.newBuilder()
                .setBatchId(batchId)
                .addAllUsers(batchUsers)
                .build();

        byte[] payload = batch.toByteArray();
        kafkaTemplate.send(userTopic, payload);
        log.info("📤 Sent batch with {} users ({} bytes)", batchUsers.size(), payload.length);
    }


    private UserProto.User mapRowToProto(String[] row) {
        if (row.length < 14) return null;

        try {
            return UserProto.User.newBuilder()
                    .setName(safe(row[1]))
                    .setAvatar(safe(row[2]))
                    .setLevel(parseInt(row[3]))
                    .setSex(safe(row[4]))
                    .setSign(safe(row[5]))
                    .setVipType(parseInt(row[6]))
                    .setVipStatus(parseInt(row[7]))
                    .setVipRole(parseInt(row[8]))
                    .setArchive(parseInt(row[9]))
                    .setFans(parseInt(row[10]))
                    .setFriend(parseInt(row[11]))
                    .setLikeNum(parseInt(row[12]))
                    .setIsSenior(parseInt(row[13]))
                    .build();
        } catch (Exception e) {
            log.warn("⚠️ Failed to parse row: {}", Arrays.toString(row));
            return null;
        }
    }

    private String safe(String s) {
        return s != null ? s : "";
    }

    private int parseInt(String s) {
        try {
            return Integer.parseInt(s.trim());
        } catch (Exception e) {
            return 0;
        }
    }
}
