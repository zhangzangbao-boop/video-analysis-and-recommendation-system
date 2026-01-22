package com.video.server.service.impl;

import com.video.server.entity.SystemNotice;
import com.video.server.entity.User;
import com.video.server.mapper.SystemNoticeMapper;
import com.video.server.mapper.UserMapper;
import com.video.server.service.SystemNoticeService;
import com.video.server.service.UserMessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 系统通知服务实现类
 */
@Service
@RequiredArgsConstructor
public class SystemNoticeServiceImpl implements SystemNoticeService {
    
    private final SystemNoticeMapper noticeMapper;
    private final UserMapper userMapper;
    private final UserMessageService messageService;
    
    @Override
    @Transactional
    public boolean createNotice(SystemNotice notice) {
        notice.setCreateTime(LocalDateTime.now());
        notice.setUpdateTime(LocalDateTime.now());
        if (notice.getStatus() == null) {
            notice.setStatus(0); // 默认草稿
        }
        return noticeMapper.insert(notice) > 0;
    }
    
    @Override
    @Transactional
    public boolean publishNotice(SystemNotice notice) {
        // 设置发布时间和状态
        notice.setPublishTime(LocalDateTime.now());
        notice.setStatus(1); // 已发布
        notice.setUpdateTime(LocalDateTime.now());
        
        // 如果通知已存在，更新；否则创建
        if (notice.getId() != null) {
            noticeMapper.updateById(notice);
        } else {
            notice.setCreateTime(LocalDateTime.now());
            noticeMapper.insert(notice);
        }
        
        // 发送消息给用户
        if (notice.getTargetType() == 0) {
            // 发送给全部用户
            List<User> allUsers = userMapper.selectAll();
            for (User user : allUsers) {
                messageService.createSystemMessage(
                        user.getId(),
                        notice.getId(),
                        notice.getTitle(),
                        notice.getContent()
                );
            }
        } else if (notice.getTargetType() == 1 && notice.getTargetUserId() != null) {
            // 发送给指定用户
            messageService.createSystemMessage(
                    notice.getTargetUserId(),
                    notice.getId(),
                    notice.getTitle(),
                    notice.getContent()
            );
        }
        
        return true;
    }
    
    @Override
    public List<SystemNotice> getPublishedNotices(Long targetUserId, Integer limit) {
        return noticeMapper.selectPublishedNotices(null, targetUserId, limit);
    }
}
