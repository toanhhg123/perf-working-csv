package com.app.performanceexport;

import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static java.lang.Integer.parseInt;
import static java.lang.Long.parseLong;

@Service
@Slf4j
public class CsvService {
    private static final int BATCH_SIZE = 5000;
    private static final int THREAD_POOL_SIZE = Runtime.getRuntime().availableProcessors();
    private final JdbcTemplate jdbcTemplate;

    public CsvService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void readLargeCSVWithStream(MultipartFile file) throws IOException {
        log.info("📥 Start reading CSV file: {}", file.getOriginalFilename());

        long startTime = System.currentTimeMillis();
        AtomicInteger totalInserted = new AtomicInteger(0);
        AtomicInteger malformedCount = new AtomicInteger(0);
        AtomicInteger submittedTasks = new AtomicInteger(0);
        List<Exception> taskExceptions = Collections.synchronizedList(new ArrayList<>());

        ExecutorService executor = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
        CompletionService<Void> completionService = new ExecutorCompletionService<>(executor);
        Semaphore semaphore = new Semaphore(THREAD_POOL_SIZE);

        try {
            CsvParser parser = getCsvParser(file);
            processRows(parser, completionService, semaphore, totalInserted, malformedCount, submittedTasks);
            waitForAllTasks(completionService, submittedTasks.get(), taskExceptions);
        } finally {
            executor.shutdown();
        }

        long duration = System.currentTimeMillis() - startTime;
        log.info("✅ DONE. Inserted {} rows in {} ms (~{} sec). Skipped {} malformed rows. Failed batches: {}",
                totalInserted.get(), duration, duration / 1000, malformedCount.get(), taskExceptions.size());
    }

    private void processRows(
            CsvParser parser,
            CompletionService<Void> completionService,
            Semaphore semaphore,
            AtomicInteger totalInserted,
            AtomicInteger malformedCount,
            AtomicInteger submittedTasks
    ) {
        String[] row;
        List<UserCsv> batch = new ArrayList<>(BATCH_SIZE);

        while ((row = parser.parseNext()) != null) {
            UserCsv user = convertRowToUserCsv(row);
            if (user == null) {
                malformedCount.incrementAndGet();
                log.warn("⚠️ Malformed row skipped: {}", Arrays.toString(row));
                continue;
            }

            batch.add(user);
            if (batch.size() >= BATCH_SIZE) {
                submitBatch(new ArrayList<>(batch), completionService, semaphore, totalInserted);
                submittedTasks.incrementAndGet();
                batch.clear();
            }
        }

        // Submit any remaining batch
        if (!batch.isEmpty()) {
            submitBatch(new ArrayList<>(batch), completionService, semaphore, totalInserted);
            submittedTasks.incrementAndGet();
        }
    }

    private void submitBatch(
            List<UserCsv> batchToInsert,
            CompletionService<Void> completionService,
            Semaphore semaphore,
            AtomicInteger totalInserted
    ) {
        try {
            semaphore.acquire(); // Chặn nếu quá giới hạn
            completionService.submit(() -> {
                try {
                    persistBatch(batchToInsert);
                    totalInserted.addAndGet(batchToInsert.size());
                    return null;
                } finally {
                    semaphore.release();
                }
            });
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("❌ Interrupted while acquiring semaphore", e);
        }
    }

    private void waitForAllTasks(
            CompletionService<Void> completionService,
            int submittedTasks,
            List<Exception> taskExceptions
    ) {
        for (int i = 0; i < submittedTasks; i++) {
            try {
                Future<Void> future = completionService.take();
                future.get(1, TimeUnit.MINUTES); // timeout cho mỗi task
            } catch (TimeoutException e) {
                log.error("⚠️ Task timed out", e);
                taskExceptions.add(e);
            } catch (ExecutionException e) {
                log.error("❌ Error while inserting batch", e.getCause());
                taskExceptions.add(e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("❌ Interrupted while waiting for task", e);
                break;
            }
        }
    }

    public static CsvParser getCsvParser(MultipartFile file) throws IOException {
        CsvParserSettings settings = new CsvParserSettings();
        settings.setMaxCharsPerColumn(10000);
        settings.setNullValue("");
        settings.setEmptyValue("");
        settings.setIgnoreLeadingWhitespaces(true);
        settings.setIgnoreTrailingWhitespaces(true);
        settings.setHeaderExtractionEnabled(true);

        var inputStreamReader = new InputStreamReader(
                file.getInputStream(),
                StandardCharsets.UTF_8
        );
        BufferedReader reader = new BufferedReader(inputStreamReader, 16 * 1024);

        CsvParser parser = new CsvParser(settings);
        parser.beginParsing(reader);
        return parser;
    }

    public UserCsv convertRowToUserCsv(String[] row) {
        if (row.length < 14) return null;

        try {
            return new UserCsv(
                    parseLong(row[0]),
                    row[1],
                    row[2],
                    parseInt(row[3]),
                    row[4],
                    row[5],
                    parseInt(row[6]),
                    parseInt(row[7]),
                    parseInt(row[8]),
                    parseInt(row[9]),
                    parseInt(row[10]),
                    parseInt(row[11]),
                    parseInt(row[12]),
                    parseInt(row[13])
            );
        } catch (Exception e) {
            return null;
        }
    }

    private void persistBatch(List<UserCsv> batch) {
        String sql = """
                    INSERT INTO public.tbl_user (
                        name, avatar, level, sex, sign, vip_type, vip_status, vip_role,
                        archive, fans, friend, like_num, is_senior
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(@NonNull PreparedStatement ps, int i) throws SQLException {
                UserCsv u = batch.get(i);
                ps.setString(1, u.getName());
                ps.setString(2, u.getAvatar());
                ps.setObject(3, u.getLevel(), Types.INTEGER);
                ps.setString(4, u.getSex());
                ps.setString(5, u.getSign());
                ps.setObject(6, u.getVipType(), Types.INTEGER);
                ps.setObject(7, u.getVipStatus(), Types.INTEGER);
                ps.setObject(8, u.getVipRole(), Types.INTEGER);
                ps.setObject(9, u.getArchive(), Types.INTEGER);
                ps.setObject(10, u.getFans(), Types.INTEGER);
                ps.setObject(11, u.getFriend(), Types.INTEGER);
                ps.setObject(12, u.getLikeNum(), Types.INTEGER);
                ps.setObject(13, u.getIsSenior(), Types.INTEGER);
            }

            @Override
            public int getBatchSize() {
                return batch.size();
            }
        });
    }
}
