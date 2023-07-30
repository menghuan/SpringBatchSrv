package cn.github.share.config;

import lombok.Data;

@Data
public class JobExecuteConfig {
    private Long minId;
    // 本次任务执行的迁移数据的最大id
    private Long executeMaxId;
    // 需要执行最终的记录最大Id
    private Long maxId;
}
