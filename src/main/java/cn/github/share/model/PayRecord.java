package cn.github.share.model;

import lombok.Data;

import java.util.Date;

@Data
public class PayRecord {
    private Long id;
    private Long userId;
    private String payDetail;
    private String payStatus;
    private Date createTime;
    private Date updateTime;
}
