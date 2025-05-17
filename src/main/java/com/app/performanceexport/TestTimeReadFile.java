package com.app.performanceexport;

import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.batch.item.file.mapping.FieldSetMapper;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Service;

import java.io.FileReader;
import java.util.List;

@Slf4j
@Service
public class TestTimeReadFile {

    public void readWithUnivocity(String filePath) throws Exception {
        long start = System.currentTimeMillis();

        CsvParserSettings settings = new CsvParserSettings();
        settings.setHeaderExtractionEnabled(true);

        CsvParser parser = new CsvParser(settings);
        List<String[]> allRows = parser.parseAll(new FileReader(filePath));

        long end = System.currentTimeMillis();
        log.info("Univocity: Đọc {} dòng trong {}ms", allRows.size(), end - start);
    }

    public void readWithSpringBatchReader(String filePath) throws Exception {
        long start = System.currentTimeMillis();


        FlatFileItemReader<UserCsv> reader = new FlatFileItemReaderBuilder<UserCsv>()
                .name("userCsvReader")
                .resource(new FileSystemResource(filePath))
                .delimited()
                .delimiter(",")
                .names("uid", "name", "avatar", "level", "sex", "sign",
                        "vip_type", "vip_status", "vip_role",
                        "archive", "fans", "friend", "like_num", "is_senior")
                .fieldSetMapper(createFieldSetMapper())
                .linesToSkip(1)
                .build();

        reader.open(new org.springframework.batch.item.ExecutionContext());

        int count = 0;
        UserCsv item;
        while ((item = reader.read()) != null) {
            count++;
        }

        reader.close();
        long end = System.currentTimeMillis();
        log.info("SpringBatch Reader: Đọc {} dòng trong {}ms", count, end - start);
    }

    private FieldSetMapper<UserCsv> createFieldSetMapper() {
        BeanWrapperFieldSetMapper<UserCsv> mapper = new BeanWrapperFieldSetMapper<>();
        mapper.setTargetType(UserCsv.class);
        return mapper;
    }
}
