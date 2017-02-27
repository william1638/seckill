create database seckill;

use seckill;

create table seckill(
  seckill_id bigint not null auto_increment comment '商品库存id',
  name VARCHAR(120) not null comment '商品名称',
  number int not null comment '库存数量',
  start_time TIMESTAMP not null comment '秒杀开启时间',
  end_time TIMESTAMP  not null comment '秒杀结束时间',
  create_time TIMESTAMP  not null default CURRENT_TIMESTAMP  comment '创建时间',
  primary key (seckill_id),
  key idx_start_time(start_time),
  key id_end_time(end_time),
  key idx_create_time(create_time)
)ENGINE=Innodb auto_increment=1000 default charset=utf8 comment='库存秒杀表';

insert INTO  seckill(name,number,start_time,end_time)
    values
      ('1000元秒杀iphone6',100,'2015-11-01 00:00:00','2015-11-02 00:00:00'),
      ('500元秒杀小米',200,'2015-11-01 00:00:00','2015-11-02 00:00:00'),
      ('1000元秒杀ipad2',100,'2015-11-01 00:00:00','2015-11-02 00:00:00'),
      ('1000元秒杀红米',300,'2015-11-01 00:00:00','2015-11-02 00:00:00');

create table success_killed(
  seckill_id BIGINT not null comment '秒杀商品id',
  user_phone bigint not null comment '用户手机号码',
  state tinyint not null default -1 comment '状态标识-1 无效 0已成功 1已付款',
  create_time TIMESTAMP not null comment '创建时间',
  PRIMARY KEY (seckill_id,user_phone),   /*联合主键*/
  key idx_create_time (create_time)
)ENGINE=Innodb auto_increment=1000 default charset=utf8 comment='库存成功明细';
