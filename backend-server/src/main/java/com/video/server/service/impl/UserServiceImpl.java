package com.video.server.service.impl;

import com.video.server.dto.PageResponse;
import com.video.server.dto.UserActivityDTO;
import com.video.server.dto.UserCreateRequest;
import com.video.server.dto.UserListRequest;
import com.video.server.dto.VideoDTO;
import com.video.server.entity.User;
import com.video.server.entity.Video;
import com.video.server.entity.VideoComment;
import com.video.server.exception.BusinessException;
import com.video.server.mapper.*;
import com.video.server.service.UserService;
import com.video.server.utils.IdGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal; // 【关键】导入 BigDecimal
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
public class UserServiceImpl implements UserService {

    @Autowired private UserMapper userMapper;
    @Autowired private VideoMapper videoMapper;
    @Autowired private VideoInteractionMapper interactionMapper;
    @Autowired private VideoCommentMapper commentMapper;

    @Override
    public User getUserById(Long id) {
        return userMapper.selectById(id);
    }

    @Override
    public User getUserByUsername(String username) {
        return userMapper.selectByUsername(username);
    }

    @Override
    public void createUser(UserCreateRequest request) {
        if (getUserByUsername(request.getUsername()) != null) {
            throw new BusinessException(400, "用户名已存在");
        }

        User user = new User();
        user.setId(IdGenerator.generateId());
        user.setUsername(request.getUsername());

        String salt = UUID.randomUUID().toString().replace("-", "");
        user.setSalt(salt);
        user.setPassword(encryptPassword(request.getPassword(), salt));

        user.setNickname(StringUtils.hasText(request.getNickname()) ? request.getNickname() : "用户" + user.getId());
        user.setPhone(request.getPhone());
        user.setEmail(request.getEmail());
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        user.setStatus(1);
        user.setStatusStr("normal");

        // 【修复】double 无法直接转 BigDecimal，需使用 BigDecimal.ZERO 或 new BigDecimal
        user.setBalance(BigDecimal.ZERO);
        user.setPoints(0);
        user.setLevel(1);

        userMapper.insert(user);
    }

    @Override
    public PageResponse<User> getUserList(UserListRequest request) {
        // 调用 Mapper (参数顺序必须匹配 UserMapper.java)
        List<User> list = userMapper.selectByCondition(
                request.getKeyword(),
                request.getStatus(),
                request.getLevel()
        );

        // 【新增】获取总数
        Long total = userMapper.countByCondition(
                request.getKeyword(),
                request.getStatus(),
                request.getLevel()
        );

        // 如果 total 为 null (空表)，处理为 0
        if (total == null) total = 0L;

        return PageResponse.of(list, total, request.getPage(), request.getPageSize());
    }

    @Override
    public void updateUserStatus(Long userId, String status) {
        User user = userMapper.selectById(userId);
        if (user == null) throw new BusinessException(404, "用户不存在");

        user.setStatusStr(status);
        if ("normal".equals(status)) user.setStatus(1);
        else if ("frozen".equals(status)) user.setStatus(0);
        else if ("muted".equals(status)) user.setStatus(2);

        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);
    }

    @Override
    public void resetPassword(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) throw new BusinessException(404, "用户不存在");

        String newSalt = UUID.randomUUID().toString().replace("-", "");
        user.setSalt(newSalt);
        user.setPassword(encryptPassword("123456", newSalt));
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);
    }

    @Override
    public List<UserActivityDTO> getUserActivities(Long userId) {
        List<UserActivityDTO> list = new ArrayList<>();
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        // 1. 注册动态
        User user = userMapper.selectById(userId);
        if (user != null && user.getCreateTime() != null) {
            list.add(new UserActivityDTO(
                    dtf.format(user.getCreateTime()),
                    "注册成为新用户",
                    "#909399",
                    "register",
                    user.getCreateTime().toEpochSecond(ZoneOffset.of("+8"))
            ));
        }

        // 2. 发布动态
        try {
            List<Video> videos = videoMapper.selectListByUserId(userId);
            if (videos != null) {
                for (Video v : videos) {
                    if (v.getCreateTime() == null) continue;
                    list.add(new UserActivityDTO(
                            dtf.format(v.getCreateTime()),
                            "发布了视频《" + v.getTitle() + "》",
                            "#409EFF",
                            "publish",
                            v.getCreateTime().toEpochSecond(ZoneOffset.of("+8"))
                    ));
                }
            }
        } catch (Exception e) {}

        // 3. 点赞动态
        try {
            List<VideoDTO> likes = interactionMapper.selectLikedVideos(userId, 10);
            if (likes != null) {
                for (VideoDTO v : likes) {
                    list.add(new UserActivityDTO(
                            "近期",
                            "点赞了视频《" + v.getTitle() + "》",
                            "#F56C6C",
                            "like",
                            System.currentTimeMillis() / 1000
                    ));
                }
            }
        } catch (Exception e) {}

        // 4. 收藏动态
        try {
            List<VideoDTO> collects = interactionMapper.selectCollectedVideos(userId, 10);
            if (collects != null) {
                for (VideoDTO v : collects) {
                    list.add(new UserActivityDTO(
                            "近期",
                            "收藏了视频《" + v.getTitle() + "》",
                            "#E6A23C",
                            "collect",
                            System.currentTimeMillis() / 1000
                    ));
                }
            }
        } catch (Exception e) {}

        // 5. 评论动态
        try {
            List<VideoComment> comments = commentMapper.selectByUserId(userId, 10);
            if (comments != null) {
                for (VideoComment c : comments) {
                    Video video = videoMapper.selectById(c.getVideoId());
                    String title = (video != null) ? video.getTitle() : "未知视频";
                    String content = c.getContent();
                    if (content != null && content.length() > 10) content = content.substring(0, 10) + "...";

                    list.add(new UserActivityDTO(
                            dtf.format(c.getCreateTime()),
                            "评论了视频《" + title + "》: " + content,
                            "#67C23A",
                            "comment",
                            c.getCreateTime().toEpochSecond(ZoneOffset.of("+8"))
                    ));
                }
            }
        } catch (Exception e) {}

        Collections.sort(list);
        if (list.size() > 20) {
            return list.subList(0, 20);
        }
        return list;
    }

    private String encryptPassword(String password, String salt) {
        String saltedPassword = password + salt;
        return DigestUtils.md5DigestAsHex(saltedPassword.getBytes(StandardCharsets.UTF_8));
    }
}