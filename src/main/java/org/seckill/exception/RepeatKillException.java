package org.seckill.exception;

/**
 * 重复秒杀异常
 * Created by William on 2017/2/27.
 */
public class RepeatKillException extends SeckillException{

    public RepeatKillException(String message) {
        super(message);
    }

    public RepeatKillException(String message, Throwable cause) {
        super(message, cause);
    }
}
