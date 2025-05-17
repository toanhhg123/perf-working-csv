package com.app.performanceexport.batch;

import com.app.performanceexport.User;
import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStream;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.ItemStreamReader;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.List;


@Slf4j
public class UnivocityCsvItemReader implements ItemStreamReader<User>, ItemStream {

    private final String filePath;
    private List<String[]> allRows;
    private int currentIndex = 0;

    public UnivocityCsvItemReader(String filePath) {
        this.filePath = filePath;
    }

    @Override
    public void open(@NonNull ExecutionContext executionContext) throws ItemStreamException {
        CsvParserSettings settings = new CsvParserSettings();
        settings.setHeaderExtractionEnabled(true);
        CsvParser parser = new CsvParser(settings);

        try {
            allRows = parser.parseAll(new FileReader(filePath));
        } catch (FileNotFoundException e) {
            log.error("File not found: {}", filePath, e);
            throw new ItemStreamException(e);
        }

    }

    @Override
    public User read() {
        if (allRows == null || currentIndex >= allRows.size()) return null;

        String[] row = allRows.get(currentIndex++);
        User user = new User();
        user.setUid(null);
        user.setName(row[1]);
        user.setAvatar(row[2]);
        user.setLevel(Integer.parseInt(row[3]));
        user.setSex(row[4]);
        user.setSign(row[5]);
        user.setVipType(Integer.parseInt(row[6]));
        user.setVipStatus(Integer.parseInt(row[7]));
        user.setVipRole(Integer.parseInt(row[8]));
        user.setArchive(Integer.parseInt(row[9]));
        user.setFans(Integer.parseInt(row[10]));
        user.setFriend(Integer.parseInt(row[11]));
        user.setLikeNum(Integer.parseInt(row[12]));
        user.setIsSenior(Integer.parseInt(row[13]));

        return user;

    }
}
