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
