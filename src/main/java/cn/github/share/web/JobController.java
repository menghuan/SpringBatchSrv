package cn.github.share.web;

import cn.github.share.task.PayRecordTask;
import lombok.SneakyThrows;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.configuration.DuplicateJobException;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.configuration.support.ReferenceJobFactory;
import org.springframework.batch.core.launch.support.SimpleJobOperator;
import org.springframework.batch.support.PropertiesConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.PostConstruct;
import java.util.Properties;

@RestController
@RequestMapping(value = "job")
public class JobController {

    @Autowired
    private PayRecordTask task;

    @Autowired
    private SimpleJobOperator jobOperator;

    @Autowired
    private Job migratePayRecordJob;

    private Long executePayRecordId;

    @Autowired
    private JobRegistry registry;

    @PostConstruct
    public void registry() throws DuplicateJobException {
        ReferenceJobFactory factory = new ReferenceJobFactory(migratePayRecordJob);
        registry.register(factory);
    }

    @PostMapping(value = "migratePayRecordJob")
    @SneakyThrows
    public ResponseEntity<String> migratePayRecordJob() {
        task.migratePayRecord();
        return ResponseEntity.ok("操作成功！");
    }

    @PostMapping(value = "splitPayRecord")
    @SneakyThrows
    public ResponseEntity<String> splitPayRecord() {
        task.splitPayRecord();
        return ResponseEntity.ok("操作成功！");
    }

    @PostMapping(value = "startJob")
    @SneakyThrows
    public ResponseEntity<String> startJob() {
        Properties properties = new Properties();
        properties.put("minId", "20");
        properties.put("maxId", "50");
        executePayRecordId = jobOperator.start("migratePayRecordJob",
                PropertiesConverter.propertiesToString(properties));
        return ResponseEntity.ok("操作成功");
    }

    @PostMapping(value = "stopJob")
    @SneakyThrows
    public ResponseEntity<String> stopJob() {
        Assert.notNull(executePayRecordId, "执行id不能为空！");
        jobOperator.stop(executePayRecordId);
        return ResponseEntity.ok("操作成功");
    }

    @PostMapping(value = "restartJob")
    @SneakyThrows
    public ResponseEntity<String> restartJob() {
        Assert.notNull(executePayRecordId, "执行id不能为空！");
        jobOperator.restart(executePayRecordId);
        return ResponseEntity.ok("操作成功");
    }

}
