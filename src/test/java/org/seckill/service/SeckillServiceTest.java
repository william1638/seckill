package org.seckill.service;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.seckill.dto.Exposer;
import org.seckill.dto.SeckillExecution;
import org.seckill.entity.Seckill;
import org.seckill.enums.SeckillStatEnum;
import org.seckill.exception.RepeatKillException;
import org.seckill.exception.SeckillCloseException;
import org.seckill.exception.SeckillException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;


import java.util.List;

import static org.junit.Assert.*;

/**
 * Created by William on 2017/2/27.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({
        "classpath:spring/spring-dao.xml",
        "classpath:spring/spring-service.xml"
    })
public class SeckillServiceTest {
    Logger logger = LoggerFactory.getLogger(this.getClass());
    @Autowired
    private SeckillService seckillService ;


    @Test
    public void getSeckillList() throws Exception {
        List<Seckill> seckillList = seckillService.getSeckillList();

        logger.info("list={}",seckillList);
    }

    @Test
    public void getById() throws Exception {
        Seckill seckill = seckillService.getById(1000);
        logger.info("list={}"+seckill);
    }

    @Test
    public void exportSeckillUrl() throws Exception {
        Exposer exposer = seckillService.exportSeckillUrl(1000);
        logger.info("list={}"+exposer);
    }

    @Test
    public void executeSeckill() throws Exception {
       String md5 = "e413a703ce18dcce375a47f563171d06";
       long userPhone = 213661399413L;
        SeckillExecution execution = null;
        try {
            execution = seckillService.executeSeckill(1000, userPhone, md5);
        } catch (SeckillCloseException e) {
            logger.error(e.getMessage());
        }catch (RepeatKillException e) {
            logger.error(e.getMessage());
        }

        logger.info("list="+execution);
    }

//    继承测试代码完整逻辑，注意可重复执行
    @Test
    public void testSeckillLogic()throws Exception{
        long id = 1000L ;
        Exposer exposer = seckillService.exportSeckillUrl(1000);
        if(exposer.isExposed()){
            logger.info("exposer=",exposer);
            long userPhone = 123235232323L ;
            String md5 = exposer.getMd5();
            try {
               SeckillExecution execution = seckillService.executeSeckill(1000, userPhone, md5);
            } catch (SeckillCloseException e) {
                logger.error(e.getMessage());
            }catch (RepeatKillException e) {
                logger.error(e.getMessage());
            }

        }else{
            //秒杀未开启
            logger.warn("exposer=",exposer);
        }

    }
    @Test
    public void executeSeckillProcedure(){
        long seckillId = 1002;
        long phone = 16456445 ;
        Exposer exposer = seckillService.exportSeckillUrl(seckillId);
        if(exposer.isExposed()){
           String md5 = exposer.getMd5();
            SeckillExecution seckillExecution = seckillService.executeSeckillProcedure(seckillId, phone, md5);
            logger.info(seckillExecution.getStateInfo());
        }
    }

}