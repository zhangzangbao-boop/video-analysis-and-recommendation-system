package com.video.server.utils;

import java.util.concurrent.atomic.AtomicLong;

/**
 * ID生成器工具类
 * 使用雪花算法生成唯一ID（简化版）
 * 或者使用时间戳+序列号的方式
 */
public class IdGenerator {
    
    // 机器ID（可以根据实际部署情况配置）
    private static final long MACHINE_ID = 1L;
    
    // 序列号
    private static final AtomicLong sequence = new AtomicLong(0);
    
    // 上次生成ID的时间戳
    private static volatile long lastTimestamp = -1L;
    
    // 序列号占用的位数
    private static final long SEQUENCE_BITS = 12L;
    
    // 机器ID占用的位数
    private static final long MACHINE_ID_BITS = 5L;
    
    // 序列号最大值
    private static final long MAX_SEQUENCE = ~(-1L << SEQUENCE_BITS);
    
    // 机器ID左移位数
    private static final long MACHINE_ID_SHIFT = SEQUENCE_BITS;
    
    // 时间戳左移位数
    private static final long TIMESTAMP_SHIFT = SEQUENCE_BITS + MACHINE_ID_BITS;
    
    /**
     * 生成下一个ID
     * @return 生成的ID
     */
    public static synchronized long nextId() {
        long timestamp = System.currentTimeMillis();
        
        if (timestamp < lastTimestamp) {
            throw new RuntimeException("时钟回退，无法生成ID");
        }
        
        if (timestamp == lastTimestamp) {
            // 同一毫秒内，序列号递增
            long seq = sequence.incrementAndGet();
            if (seq > MAX_SEQUENCE) {
                // 序列号溢出，等待下一毫秒
                timestamp = waitNextMillis(lastTimestamp);
                sequence.set(0);
            }
        } else {
            // 新的毫秒，序列号重置
            sequence.set(0);
        }
        
        lastTimestamp = timestamp;
        
        // 组装ID：时间戳 + 机器ID + 序列号
        return ((timestamp << TIMESTAMP_SHIFT)
                | (MACHINE_ID << MACHINE_ID_SHIFT)
                | sequence.get());
    }
    
    /**
     * 等待下一毫秒
     */
    private static long waitNextMillis(long lastTimestamp) {
        long timestamp = System.currentTimeMillis();
        while (timestamp <= lastTimestamp) {
            timestamp = System.currentTimeMillis();
        }
        return timestamp;
    }
    
    /**
     * 简化版ID生成（使用时间戳+随机数，适合单机环境）
     * @return 生成的ID
     */
    public static long generateSimpleId() {
        // 时间戳（毫秒）左移20位 + 随机数（20位）
        long timestamp = System.currentTimeMillis();
        long random = (long)(Math.random() * (1L << 20));
        return (timestamp << 20) | random;
    }
}
