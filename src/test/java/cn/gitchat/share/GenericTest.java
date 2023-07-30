package cn.github.share;

import cn.github.share.config.JobExecuteConfig;
import com.alibaba.fastjson.JSON;
import org.junit.jupiter.api.Test;

public class GenericTest {

    @Test
    public void generateConfig() {
        JobExecuteConfig config = new JobExecuteConfig();
        config.setExecuteMaxId(20L);
        config.setMinId(1L);
        config.setMaxId(30L);
        System.out.println(JSON.toJSONString(config));
    }
}
