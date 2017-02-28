package org.seckill.dao.cache;

import io.protostuff.LinkedBuffer;
import io.protostuff.ProtostuffIOUtil;
import io.protostuff.runtime.RuntimeSchema;
import org.seckill.entity.Seckill;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

/**
 * Created by William on 2017/2/28.
 */
public class RedisDao {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private  final JedisPool jedisPool ;

    private static RuntimeSchema<Seckill> schema = RuntimeSchema.createFrom(Seckill.class);


    public RedisDao(String host, int port) {

        jedisPool = new JedisPool( host, port);
    }

    public Seckill getSeckill(long seckillId ){
//        redis操作逻辑
        try {
          Jedis jedis = jedisPool.getResource();
            try {
                String key = "seckill:"+seckillId;
//                并没有实现内部序列化操作
//                get->byte[]->反序列化->Object(Seckill)
//                采用自定义序列化
//                protostuff:pojo
                byte[] bytes = jedis.get(key.getBytes());
                if(bytes!=null){
//          反序列化
                    Seckill seckill = schema.newMessage();
                    ProtostuffIOUtil.mergeFrom(bytes,seckill,schema);
                    return seckill ;
                }
            } finally {
                jedis.close();
            }
        } catch (Exception e) {
            logger.info(e.getMessage());
        }
        return null ;
    }

    public String putSeckill(Seckill seckill){
//        set Object(Seckill) -> 序列化 ->byte[]
        try {
            Jedis jedis = jedisPool.getResource();
            try {
                String key = "seckill:"+seckill.getSeckillId();
                byte[] bytes = ProtostuffIOUtil.toByteArray(seckill, schema, LinkedBuffer.allocate(LinkedBuffer.DEFAULT_BUFFER_SIZE));
//                超时缓存
                int timeout = 60 * 60 ;//1小时
                String result = jedis.setex(key.getBytes(), timeout, bytes);
                return result ;
            }finally {
                jedis.close();
            }
        }catch (Exception e) {
            logger.error(e.getMessage());
        }
        return null ;
    }

}
