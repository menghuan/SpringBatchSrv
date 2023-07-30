package cn.github.share.model;

import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

@Component
public class PayRecordExtProcessor implements ItemProcessor<PayRecord, PayRecordExt> {

    @Override
    public PayRecordExt process(PayRecord payRecord) throws Exception {
        // 此处注入 User service 通过userService .getId(payRecord.getUserId());
        PayRecordExt ext = new PayRecordExt();
        return ext;
    }
}
