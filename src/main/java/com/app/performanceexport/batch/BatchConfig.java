package com.app.performanceexport.batch;

import com.app.performanceexport.User;
import lombok.AllArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemStreamReader;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;


@Configuration
@EnableBatchProcessing
@AllArgsConstructor
public class BatchConfig {
    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;

    @Bean
    @StepScope
    public ItemStreamReader<User> userUnivocityReader(
            @Value("#{jobParameters['filePath']}") String filePath
    ) {
        return new UnivocityCsvItemReader(filePath);
    }

    @Bean
    @StepScope
    public FlatFileItemReader<User> reader(@Value("#{jobParameters['filePath']}") String filePath) {
        var mapper = new BeanWrapperFieldSetMapper<User>();
        mapper.setTargetType(User.class);
        return new FlatFileItemReaderBuilder<User>()
                .name("userItemReader")
                .resource(new FileSystemResource(filePath))
                .delimited()
                .delimiter(",")
                .names(
                        "uid", "name", "avatar",
                        "level", "sex", "sign", "vipType",
                        "vipStatus", "vipRole", "archive",
                        "fans", "friend", "likeNum",
                        "isSenior"
                )
                .fieldSetMapper(mapper)
                .linesToSkip(1)
                .build();
    }

    @Bean(name = "userImportTaskExecutor")
    public TaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("user-import-");
        executor.initialize();
        return executor;
    }

    @Bean
    public JdbcBatchItemWriter<User> jdbcWriter(DataSource dataSource) {
        final String sql = """
                INSERT INTO tbl_user (name, avatar, level, sex, sign, vip_type, vip_status, vip_role, archive, fans, friend, like_num, is_senior)
                VALUES (:name, :avatar, :level, :sex, :sign, :vipType, :vipStatus, :vipRole, :archive, :fans, :friend, :likeNum, :isSenior)
                """;
        return new JdbcBatchItemWriterBuilder<User>()
                .dataSource(dataSource)
                .sql(sql)
                .beanMapped()
                .build();
    }

    @Bean
    public Step userImportStep(
            @Qualifier("userUnivocityReader") ItemStreamReader<User> reader,
            JdbcBatchItemWriter<User> writer
    ) {
        return new StepBuilder("userImportStep", jobRepository)
                .<User, User>chunk(5000, transactionManager)
                .taskExecutor(taskExecutor())
                .reader(reader)
                .writer(writer)
                .build();
    }


    @Bean
    public Job importUserJob(Step userImportStep, TempFileCleanupListener listener) {
        return new JobBuilder("importUserJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .listener(listener)
                .start(userImportStep)
                .build();
    }
}
