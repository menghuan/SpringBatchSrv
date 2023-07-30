package cn.github.share.model;

import org.springframework.jdbc.core.RowMapper;
import java.sql.ResultSet;
import java.sql.SQLException;

public class PayRecordRowMapper implements RowMapper<PayRecord> {

    @Override
    public PayRecord mapRow(ResultSet resultSet, int i) throws SQLException {
        PayRecord payRecord = new PayRecord();
        payRecord.setId(resultSet.getLong("id"));
        payRecord.setPayStatus(resultSet.getString("pay_status"));
        payRecord.setUserId(resultSet.getLong("user_id"));
        payRecord.setPayDetail(resultSet.getString("pay_detail"));
        payRecord.setCreateTime(resultSet.getTimestamp("create_time"));
        payRecord.setUpdateTime(resultSet.getTimestamp("update_time"));
        return payRecord;
    }
}
