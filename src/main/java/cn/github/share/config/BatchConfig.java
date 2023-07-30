package cn.github.share.config;

import javax.sql.DataSource;
import cn.github.share.model.PayRecord;
import cn.github.share.model.PayRecordRowMapper;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.DefaultBatchConfigurer;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.support.JobRepositoryFactoryBean;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.BeanPropertyItemSqlParameterSourceProvider;
import org.springframework.batch.item.database.JdbcPagingItemReader;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.item.database.support.SqlPagingQueryProviderFactoryBean;
import org.springframework.batch.item.support.ClassifierCompositeItemWriter;
import org.springframework.batch.item.support.CompositeItemWriter;
import org.springframework.batch.item.support.PassThroughItemProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.classify.Classifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import java.util.ArrayList;
import java.util.List;

@Configuration
public class BatchConfig extends DefaultBatchConfigurer {

    @Autowired
    private JobBuilderFactory jobBuilderFactory;

    @Autowired
    private StepBuilderFactory stepBuilderFactory;

    @Autowired
    @Qualifier("targetDatasource")
    private DataSource targetDatasource;

    @Autowired
    @Qualifier("primaryDatasource")
    private DataSource primaryDatasource;

    @Autowired
    private MigrateConfig config;

    // 将spring batch 的记录存取在主数据源中
    @Override
    protected JobRepository createJobRepository() throws Exception {
        JobRepositoryFactoryBean factory = new JobRepositoryFactoryBean();
        factory.setDataSource(primaryDatasource);
        factory.setTransactionManager(this.getTransactionManager());
        factory.afterPropertiesSet();
        return factory.getObject();
    }

    /**
     * 数据读取 根据id 查询保证性能 分页读取
     */
    @Bean
    @StepScope
    public JdbcPagingItemReader<PayRecord> payRecordReader(@Value("#{jobParameters[minId]}") Long minId,
            @Value("#{jobParameters[maxId]}") Long maxId) throws Exception {
        JdbcPagingItemReader<PayRecord> reader = new JdbcPagingItemReader();
        final SqlPagingQueryProviderFactoryBean sqlPagingQueryProviderFactoryBean = new SqlPagingQueryProviderFactoryBean();
        sqlPagingQueryProviderFactoryBean.setDataSource(primaryDatasource);
        sqlPagingQueryProviderFactoryBean.setSelectClause("select * ");
        sqlPagingQueryProviderFactoryBean.setFromClause("from pay_record");
        sqlPagingQueryProviderFactoryBean.setWhereClause("id > " + minId + " and id <= " + maxId);
        sqlPagingQueryProviderFactoryBean.setSortKey("id");
        reader.setQueryProvider(sqlPagingQueryProviderFactoryBean.getObject());
        reader.setDataSource(primaryDatasource);
        reader.setPageSize(config.getPageSize());
        reader.setRowMapper(new PayRecordRowMapper());
        reader.afterPropertiesSet();
        reader.setSaveState(true);
        return reader;
    }

    /**
     * 写入到新库
     */
    @Bean
    public ItemWriter<? super PayRecord> targetPayRecordWriter() {
        return new JdbcBatchItemWriterBuilder<PayRecord>()
                .dataSource(targetDatasource)
                .itemSqlParameterSourceProvider(new BeanPropertyItemSqlParameterSourceProvider<>())
                .sql(
                        "INSERT INTO `pay_record` (\n"
                                + "`id`,\n"
                                + "`user_id`,\n"
                                + "`pay_detail`,\n"
                                + "`pay_status`,\n"
                                + "`create_time`,\n"
                                + "`update_time`\n"
                                + ")\n"
                                + "VALUES\n"
                                + "\t(\n"
                                + ":id,"
                                + ":userId,"
                                + ":payDetail,"
                                + ":payStatus,"
                                + ":createTime,"
                                + ":updateTime"
                                + ")")
                .build();
    }

    /**
     * 删除器
     *
     * @return
     */
    @Bean
    public ItemWriter<PayRecord> deletePayRecordWriter() {
        return new JdbcBatchItemWriterBuilder<PayRecord>()
                .dataSource(primaryDatasource)
                .itemSqlParameterSourceProvider(new BeanPropertyItemSqlParameterSourceProvider<>())
                .sql("delete from pay_record where id = :id")
                .build();
    }

    @Bean
    public CompositeItemWriter<PayRecord> compositePayRecordItemWriter(
            @Qualifier("deletePayRecordWriter") ItemWriter deleteWriter,
            @Qualifier("targetPayRecordWriter") ItemWriter targetPayRecordWriter) {
        CompositeItemWriter<PayRecord> compositeItemWriter = new CompositeItemWriter<>();
        List<ItemWriter<? super PayRecord>> list = new ArrayList<>();
        list.add(deleteWriter);
        list.add(targetPayRecordWriter);
        compositeItemWriter.setDelegates(list);
        return compositeItemWriter;
    }

    @Bean
    public Step migratePayRecordStep(@Qualifier("payRecordReader") JdbcPagingItemReader<PayRecord> payRecordReader,
            @Qualifier(value = "compositePayRecordItemWriter") CompositeItemWriter compositeItemWriter) {
        return this.stepBuilderFactory.get("migratePayRecordStep")
                .<PayRecord, PayRecord>chunk(config.getChunkSize())
                .reader(payRecordReader)
                .processor(new PassThroughItemProcessor())
                .writer(compositeItemWriter)
                .taskExecutor(new SimpleAsyncTaskExecutor("migrate_thread"))
                .throttleLimit(config.getThreadSize())
                .build();
    }

    @Bean
    public Job migratePayRecordJob(@Qualifier("migratePayRecordStep") Step step) {
        return this.jobBuilderFactory.get("migratePayRecordJob")
                .start(step)
                .incrementer(new RunIdIncrementer())
                .build();
    }

    @Bean
    public ItemWriter<? super PayRecord> payRecordOneWriter() {
        return new JdbcBatchItemWriterBuilder<PayRecord>()
                .dataSource(primaryDatasource)
                .itemSqlParameterSourceProvider(new BeanPropertyItemSqlParameterSourceProvider<>())
                .sql(
                        "INSERT INTO `pay_record_1` (\n"
                                + "`id`,\n"
                                + "`user_id`,\n"
                                + "`pay_detail`,\n"
                                + "`pay_status`,\n"
                                + "`create_time`,\n"
                                + "`update_time`\n"
                                + ")\n"
                                + "VALUES\n"
                                + "\t(\n"
                                + ":id,"
                                + ":userId,"
                                + ":payDetail,"
                                + ":payStatus,"
                                + ":createTime,"
                                + ":updateTime"
                                + ")")
                .build();
    }

    @Bean
    public ItemWriter<? super PayRecord> payRecordTwoWriter() {
        return new JdbcBatchItemWriterBuilder<PayRecord>()
                .dataSource(primaryDatasource)
                .itemSqlParameterSourceProvider(new BeanPropertyItemSqlParameterSourceProvider<>())
                .sql(
                        "INSERT INTO `pay_record_2` (\n"
                                + "`id`,\n"
                                + "`user_id`,\n"
                                + "`pay_detail`,\n"
                                + "`pay_status`,\n"
                                + "`create_time`,\n"
                                + "`update_time`\n"
                                + ")\n"
                                + "VALUES\n"
                                + "\t(\n"
                                + ":id,"
                                + ":userId,"
                                + ":payDetail,"
                                + ":payStatus,"
                                + ":createTime,"
                                + ":updateTime"
                                + ")")
                .build();
    }

    // 根据条件拆分为多表
    @Bean
    public ClassifierCompositeItemWriter<? super PayRecord> classifierItemWriter(
            @Qualifier("payRecordOneWriter") ItemWriter payRecordOneWriter,
            @Qualifier("payRecordTwoWriter") ItemWriter payRecordTwoWriter) {
        ClassifierCompositeItemWriter<PayRecord> classifierCompositeItemWriter = new ClassifierCompositeItemWriter<>();
        classifierCompositeItemWriter.setClassifier(
                (Classifier<PayRecord, ItemWriter<? super PayRecord>>) record -> {
                    ItemWriter<? super PayRecord> itemWriter;
                    if (record.getId() % 2 == 0) {
                        itemWriter = payRecordOneWriter;
                    } else {
                        itemWriter = payRecordTwoWriter;
                    }
                    return itemWriter;
                });
        return classifierCompositeItemWriter;
    }

    @Bean
    public Step splitPayRecordStep(@Qualifier("payRecordReader") JdbcPagingItemReader<PayRecord> payRecordReader,
            @Qualifier(value = "classifierItemWriter") ClassifierCompositeItemWriter itemWriter) {
        return this.stepBuilderFactory.get("splitPayRecordStep")
                .<PayRecord, PayRecord>chunk(config.getChunkSize())
                .reader(payRecordReader)
                .processor(new PassThroughItemProcessor())
                .writer(itemWriter)
                .taskExecutor(new SimpleAsyncTaskExecutor("migrate_thread"))
                .throttleLimit(config.getThreadSize())
                .build();
    }

    @Bean
    public Job splitPayRecordJob(@Qualifier("splitPayRecordStep") Step step) {
        return this.jobBuilderFactory.get("splitPayRecordJob")
                .start(step)
                .incrementer(new RunIdIncrementer())
                .build();
    }

}
