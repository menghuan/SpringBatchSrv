package cn.github.share;

import cn.github.share.config.MigrateConfig;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

import javax.sql.DataSource;

@SpringBootApplication
@EnableBatchProcessing
@EnableConfigurationProperties(value = { MigrateConfig.class })
@EnableScheduling
@EnableSchedulerLock(defaultLockAtMostFor = "30s")
public class BatchSrvApplication {

    public static void main(String[] args) {
        SpringApplication.run(BatchSrvApplication.class, args);
    }

    @Bean
    public LockProvider lockProvider(@Qualifier(value = "primaryDatasource") DataSource dataSource) {
        return new JdbcTemplateLockProvider(dataSource);
    }

}
