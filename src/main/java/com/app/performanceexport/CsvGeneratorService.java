package com.app.performanceexport;

import com.github.javafaker.Faker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
public class CsvGeneratorService {
    private static final int TOTAL_USERS = 1_000_000;
    private static final String OUTPUT_PATH = "users_fake.csv";

    public void generateFakeUsersCsv() {
        Faker faker = new Faker(Locale.ENGLISH);
        long start = System.currentTimeMillis();

        try (PrintWriter writer = new PrintWriter(
                new BufferedWriter(new FileWriter(OUTPUT_PATH, false)), true)) {

            writer.println("uid,name,avatar,level,sex,sign,vip_type,vip_status,vip_role,archive,fans,friend,like_num,is_senior");

            for (int i = 1; i <= TOTAL_USERS; i++) {
                String line = String.join(",",
                        String.valueOf(i),
                        faker.internet().emailAddress(),
                        "https://example.com/avatar/" + i + ".jpg",
                        String.valueOf(randomInt(1, 100)),
                        faker.options().option("male", "female", "other"),
                        faker.lorem().sentence().replace(",", " "),
                        String.valueOf(randomInt(0, 2)),
                        String.valueOf(randomInt(0, 1)),
                        String.valueOf(randomInt(0, 1)),
                        String.valueOf(randomInt(0, 500)),
                        String.valueOf(randomInt(0, 10_000)),
                        String.valueOf(randomInt(0, 10_000)),
                        String.valueOf(randomInt(0, 10_000)),
                        String.valueOf(randomInt(0, 1))
                );

                writer.println(line);

                if (i % 100_000 == 0) {
                    log.info("Generated {} users...", i);
                }
            }

            log.info("Done generating {} users in {} ms", TOTAL_USERS, System.currentTimeMillis() - start);

        } catch (Exception e) {
            log.error("Error generating fake CSV", e);
        }
    }

    private int randomInt(int min, int max) {
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }
}
