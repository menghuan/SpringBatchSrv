create table pay_record
(
	id bigint auto_increment
		primary key,
	user_id bigint null,
	pay_detail varchar(500) null,
	pay_status varchar(20) null,
	create_time timestamp null,
	update_time timestamp null
);


CREATE TABLE `job_config` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `config_key` varchar(255) NOT NULL,
  `config_value` text COMMENT '配置值',
  `description` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4;

INSERT INTO job_config (id, config_key, config_value, description) VALUES (6, 'pay_record_job_config', '{"executeMaxId":201,"maxId":30,"minId":1}', '迁移pay_record 的配置');
CREATE TABLE shedlock(
    name VARCHAR(64),
    lock_until TIMESTAMP(3) NULL,
    locked_at TIMESTAMP(3) NULL,
    locked_by  VARCHAR(255),
    PRIMARY KEY (name)
)

create TABLE  pay_record_1 like  pay_record;

create TABLE  pay_record_2 like pay_record;