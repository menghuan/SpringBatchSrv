package cn.github.share.task;

import cn.github.share.config.JobExecuteConfig;
import cn.github.share.constants.MigrateConstants;
import com.alibaba.fastjson.JSON;

import java.time.Instant;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class PayRecordTask {

    @Autowired
    private Job migratePayRecordJob;

    @Autowired
    private Job splitPayRecordJob;

    @Autowired
    private JobLauncher jobLauncher;

    @Autowired
    @Qualifier("primaryTemplate")
    private JdbcTemplate primaryJdbcTemplate;

    @Autowired
    private Environment env;

    /**
     * 迁移任务
     * 
     * @throws Exception
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void migratePayRecord() throws Exception {
        Map<String, Object> result = primaryJdbcTemplate
                .queryForMap("select * from job_config where config_key = ?",
                        MigrateConstants.PAY_RECORD_JOB_CONFIG_KEY);
        String value = (String) result.get("config_value");
        JobExecuteConfig payRecordJobConfig = JSON.parseObject(value, JobExecuteConfig.class);
        JobParameters params = new JobParametersBuilder()
                .addLong("maxId", payRecordJobConfig.getExecuteMaxId())
                .addLong("minId", payRecordJobConfig.getMinId())
                .toJobParameters();
        jobLauncher.run(migratePayRecordJob, params);
    }

    /**
     *
     * 执行迁移任务之前数据迁移配置初始化
     * 凌晨一点
     */
    @Scheduled(fixedRate = 10000)
    @SchedulerLock(name = "migrate_pay_config")
    public void initMigrateConfig() {
        System.out.println("=======" + env.getProperty("server.port") + "|||" + Instant.now().toEpochMilli());
        // TODO 根据实际需要的业务逻辑动态计算出，每次要迁移的数据是哪些 ，最好根据时间算出MaxId, 然后每天执行固定的步长 ，比如每天迁移100万
    }

    @Scheduled(cron = "0 0 2 * * ?")
    public void splitPayRecord() throws Exception {
        Map<String, Object> result = primaryJdbcTemplate
                .queryForMap("select * from job_config where config_key = ?",
                        MigrateConstants.PAY_RECORD_JOB_CONFIG_KEY);
        String value = (String) result.get("config_value");
        JobExecuteConfig payRecordJobConfig = JSON.parseObject(value, JobExecuteConfig.class);
        JobParameters params = new JobParametersBuilder()
                .addLong("maxId", payRecordJobConfig.getExecuteMaxId())
                .addLong("minId", payRecordJobConfig.getMinId())
                .toJobParameters();
        jobLauncher.run(splitPayRecordJob, params);
    }

}
