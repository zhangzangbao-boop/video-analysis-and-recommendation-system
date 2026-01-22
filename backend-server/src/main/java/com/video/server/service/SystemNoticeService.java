package com.video.server.service;

import com.video.server.entity.SystemNotice;

import java.util.List;

/**
 * 系统通知服务接口
 */
public interface SystemNoticeService {
    
    /**
     * 创建系统通知
     * @param notice 通知对象
     * @return 是否成功
     */
    boolean createNotice(SystemNotice notice);
    
    /**
     * 发布系统通知（创建通知并发送给所有用户或指定用户）
     * @param notice 通知对象
     * @return 是否成功
     */
    boolean publishNotice(SystemNotice notice);
    
    /**
     * 获取已发布的通知列表
     * @param targetUserId 目标用户ID（可选）
     * @param limit 限制数量
     * @return 通知列表
     */
    List<SystemNotice> getPublishedNotices(Long targetUserId, Integer limit);
}
