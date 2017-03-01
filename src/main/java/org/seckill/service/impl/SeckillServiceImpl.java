package org.seckill.service.impl;

import org.apache.commons.collections.MapUtils;
import org.seckill.dao.SeckillDao;
import org.seckill.dao.SuccessKilledDao;
import org.seckill.dao.cache.RedisDao;
import org.seckill.dto.Exposer;
import org.seckill.dto.SeckillExecution;
import org.seckill.entity.Seckill;
import org.seckill.entity.SuccessKilled;
import org.seckill.enums.SeckillStatEnum;
import org.seckill.exception.RepeatKillException;
import org.seckill.exception.SeckillCloseException;
import org.seckill.exception.SeckillException;
import org.seckill.service.SeckillService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;

import javax.annotation.Resource;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by William on 2017/2/27.
 */
@Service
@Transactional
/**
 *使用注解控制事务方法的优点：
 * 1：开发团队达成一致约定，明确标注事务方法的编程风格
 * 2：保证事务方法的执行时间竟可能断，不要穿插其他网络操作RPC/HTTP请求或者剥离到事务方法外
 * 3：不是所有的方法都需要事务，如只有一条修改操作，只读操作不需要事务控制
 */
public class SeckillServiceImpl implements SeckillService {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    private final String salt = "sfsfjojfwoj324jljljfsj";
    @Resource
    private SeckillDao seckillDao;


    @Resource
    private SuccessKilledDao successKilledDao;
    @Autowired
    private RedisDao redisDao ;

    @Override
    public List<Seckill> getSeckillList() {
        return seckillDao.queryAll(0, 3);
    }

    @Override
    public Seckill getById(long seckillId) {
        return seckillDao.queryById(seckillId);
    }

    @Override
    public Exposer exportSeckillUrl(long seckillId) {
//        缓存优化
        /**
         * get from cache
         * is null
         * get db
         *  else
         *      put cache
         */
//       1 访问redis
        Seckill seckill = redisDao.getSeckill(seckillId);
        if(seckill==null){
//           2 访问数据库
            seckill = seckillDao.queryById(seckillId);
            if(seckill==null){
                return new Exposer(false,seckillId);
            }else{
//                3 放入redis
                redisDao.putSeckill(seckill);
            }
        }
//        没查询到
        if (seckill == null) {
            return new Exposer(false, seckillId);
        }
//        没开启
        Date startTime = seckill.getStartTime();
        Date endTime = seckill.getEndTime();
        Date nowTime = new Date();
        if (startTime.getTime() > nowTime.getTime() || endTime.getTime() < nowTime.getTime()) {
            return new Exposer(false, seckillId, nowTime.getTime(), startTime.getTime(), endTime.getTime());
        }
//    秒杀开启，返回秒杀商品id、用给接口加密的md5
        String md5 = getMD5(seckillId);
        return new Exposer(true, md5, seckillId);


    }

    /**
     * @param seckillId
     * @param userPhone
     * @param md5
     * @throws SeckillException
     * @throws RepeatKillException
     * @throws SeckillCloseException
     */
    @Override
    public SeckillExecution executeSeckill(long seckillId, long userPhone, String md5) throws SeckillException, RepeatKillException, SeckillCloseException {
        if (md5 == null || !md5.equals(getMD5(seckillId))) {
            throw new SeckillException("seckill data rewrite");
        }

//        执行秒杀逻辑，减库存+增加购买明细
        Date nowTime = new Date();

        try {
            int insertCount = successKilledDao.insertSuccessKilled(seckillId, userPhone);
            //            刊是否被重复插入，及用户重复秒杀
            if (insertCount <= 0) {
                throw new RepeatKillException("seckill is repeated ");
            } else {
//              减库存
                int updateCount = seckillDao.reduceNumber(seckillId, nowTime);
                if (updateCount <= 0) {
                    //            没有更新库存记录，说明秒杀结束 rollback
                    throw new SeckillCloseException("seckill is closed");
                } else {
                    //            否则更新了库存，秒杀成功，增加明细 commit
                    SuccessKilled successKilled = successKilledDao.queryByIdWithSeckill(seckillId, userPhone);
                    return new SeckillExecution(seckillId, SeckillStatEnum.SUCCESS,successKilled);
                }
            }
        } catch (SeckillCloseException e1) {
            throw e1;
        } catch (RepeatKillException e2) {
            throw e2 ;
        }catch (Exception e){
            logger.error(e.getMessage());
//            运行异常转化为运行期异常
            throw new SeckillException("seckill inner error :"+e.getMessage());
        }

    }

    /**
     *
     * @param seckillId
     * @param userPhone
     * @param md5
     * @return
     * @throws SeckillException
     * @throws RepeatKillException
     * @throws SeckillCloseException
     */
    @Override
    public SeckillExecution executeSeckillProcedure(long seckillId, long userPhone, String md5) throws SeckillException, RepeatKillException, SeckillCloseException {
        if(md5==null || !md5.equals(getMD5(seckillId))){
            return new SeckillExecution(seckillId,SeckillStatEnum.DATA_REWRITE);
        }
        Date killTime = new Date();
        Map<String,Object> map = new HashMap<String,Object>();
        map.put("seckillId",seckillId);
        map.put("phone",userPhone);
        map.put("killTime",killTime);
        map.put("result",null);

        try {
            seckillDao.killByProcedure(map);
            Integer result = MapUtils.getInteger(map, "result");
            if(result==1){
                SuccessKilled successKilled = successKilledDao.queryByIdWithSeckill(seckillId,userPhone);
                return new SeckillExecution(seckillId,SeckillStatEnum.SUCCESS,successKilled);
            }else{
                return new SeckillExecution(seckillId,SeckillStatEnum.stateOf(result));
            }
        } catch (Exception e) {
            logger.error(e.getMessage(),e);
            return new SeckillExecution(seckillId,SeckillStatEnum.INNER_ERROR);
        }


    }

    private String getMD5(long seckillId) {
        String base = salt + seckillId;
        String md5 = DigestUtils.md5DigestAsHex(base.getBytes());
        return md5;

    }


}
