package cn.github.share.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "migrate.config")
@Data
public class MigrateConfig {
    // 每次读取数据条数
    private Integer pageSize;
    // 每次事务提交记录数
    private Integer chunkSize;
    private Integer threadSize;
}
