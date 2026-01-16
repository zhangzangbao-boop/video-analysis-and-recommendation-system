package com.video.server.controller;

import com.video.server.dto.ApiResponse;
import com.video.server.dto.PasswordChangeRequest;
import com.video.server.dto.UserUpdateRequest;
import com.video.server.entity.User;
import com.video.server.entity.UserBehavior;
import com.video.server.entity.UserFollow;
import com.video.server.entity.Video;
import com.video.server.exception.BusinessException;
import com.video.server.mapper.UserBehaviorMapper;
import com.video.server.mapper.UserFollowMapper;
import com.video.server.mapper.UserMapper;
import com.video.server.mapper.VideoMapper;
import com.video.server.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 用户端控制器
 */
@RestController
@RequestMapping("/api/v1/user")
@CrossOrigin(origins = "*", maxAge = 3600)
@RequiredArgsConstructor
public class UserController {
    
    private final UserService userService;
    private final UserMapper userMapper;
    private final UserBehaviorMapper userBehaviorMapper;
    private final UserFollowMapper userFollowMapper;
    private final VideoMapper videoMapper;
    
    /**
     * 获取当前用户信息
     */
    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<User>> getCurrentUser(HttpServletRequest request) {
        Long userId = getUserIdFromRequest(request);
        User user = userService.getUserById(userId);
        // 清除敏感信息
        user.setPassword(null);
        user.setSalt(null);
        return ResponseEntity.ok(ApiResponse.success(user));
    }
    
    /**
     * 更新用户信息
     */
    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<User>> updateProfile(
            @RequestBody UserUpdateRequest request,
            HttpServletRequest httpRequest) {
        Long userId = getUserIdFromRequest(httpRequest);
        User user = userService.getUserById(userId);
        
        // 更新允许修改的字段
        if (request.getNickname() != null) {
            user.setNickname(request.getNickname());
        }
        if (request.getAvatarUrl() != null) {
            user.setAvatarUrl(request.getAvatarUrl());
        }
        if (request.getRealName() != null) {
            user.setRealName(request.getRealName());
        }
        if (request.getEmail() != null) {
            user.setEmail(request.getEmail());
        }
        if (request.getGender() != null) {
            user.setGender(request.getGender());
        }
        if (request.getBio() != null) {
            user.setBio(request.getBio());
        }
        user.setUpdateTime(LocalDateTime.now());
        
        // 更新数据库
        userMapper.updateById(user);
        
        // 清除敏感信息
        user.setPassword(null);
        user.setSalt(null);
        return ResponseEntity.ok(ApiResponse.success(user));
    }
    
    /**
     * 修改密码
     */
    @PutMapping("/password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @RequestBody PasswordChangeRequest request,
            HttpServletRequest httpRequest) {
        Long userId = getUserIdFromRequest(httpRequest);
        User user = userService.getUserById(userId);
        
        // 验证旧密码
        if (!verifyPassword(request.getOldPassword(), user.getPassword(), user.getSalt())) {
            throw new BusinessException(400, "旧密码错误");
        }
        
        // 生成新盐值并加密新密码
        String newSalt = UUID.randomUUID().toString().replace("-", "");
        String encryptedPassword = encryptPassword(request.getNewPassword(), newSalt);
        
        // 更新密码
        userMapper.updatePasswordById(userId, encryptedPassword, newSalt);
        
        return ResponseEntity.ok(ApiResponse.success());
    }
    
    /**
     * 获取播放历史
     */
    @GetMapping("/history")
    public ResponseEntity<ApiResponse<List<Video>>> getPlayHistory(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize,
            HttpServletRequest request) {
        Long userId = getUserIdFromRequest(request);
        
        // 从user_behavior表查询观看记录（actionType='view'）
        List<UserBehavior> behaviors = userBehaviorMapper.selectByUserIdAndActionType(userId, "view");
        
        // 获取视频ID列表
        List<Long> videoIds = behaviors.stream()
                .map(UserBehavior::getVideoId)
                .distinct()
                .collect(Collectors.toList());
        
        // 查询视频信息
        List<Video> videos = videoIds.stream()
                .map(videoMapper::selectById)
                .filter(video -> video != null)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(ApiResponse.success(videos));
    }
    
    /**
     * 获取点赞记录
     */
    @GetMapping("/likes")
    public ResponseEntity<ApiResponse<List<Video>>> getLikes(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize,
            HttpServletRequest request) {
        Long userId = getUserIdFromRequest(request);
        
        // 从user_behavior表查询点赞记录（actionType='like'）
        List<UserBehavior> behaviors = userBehaviorMapper.selectByUserIdAndActionType(userId, "like");
        
        // 获取视频ID列表
        List<Long> videoIds = behaviors.stream()
                .map(UserBehavior::getVideoId)
                .distinct()
                .collect(Collectors.toList());
        
        // 查询视频信息
        List<Video> videos = videoIds.stream()
                .map(videoMapper::selectById)
                .filter(video -> video != null)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(ApiResponse.success(videos));
    }
    
    /**
     * 获取评论记录（简化版，返回评论的视频列表）
     */
    @GetMapping("/comments")
    public ResponseEntity<ApiResponse<List<Video>>> getComments(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize,
            HttpServletRequest request) {
        Long userId = getUserIdFromRequest(request);
        
        // 从user_behavior表查询评论记录（actionType='comment'）
        List<UserBehavior> behaviors = userBehaviorMapper.selectByUserIdAndActionType(userId, "comment");
        
        // 获取视频ID列表
        List<Long> videoIds = behaviors.stream()
                .map(UserBehavior::getVideoId)
                .distinct()
                .collect(Collectors.toList());
        
        // 查询视频信息
        List<Video> videos = videoIds.stream()
                .map(videoMapper::selectById)
                .filter(video -> video != null)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(ApiResponse.success(videos));
    }
    
    /**
     * 关注用户
     */
    @PostMapping("/follow/{followUserId}")
    public ResponseEntity<ApiResponse<Void>> followUser(
            @PathVariable Long followUserId,
            HttpServletRequest request) {
        Long userId = getUserIdFromRequest(request);
        
        if (userId.equals(followUserId)) {
            throw new BusinessException(400, "不能关注自己");
        }
        
        // 检查是否已关注
        UserFollow existing = userFollowMapper.selectByUserIdAndFollowUserId(userId, followUserId);
        if (existing != null) {
            throw new BusinessException(400, "已经关注过该用户");
        }
        
        // 创建关注关系
        UserFollow follow = new UserFollow();
        follow.setUserId(userId);
        follow.setFollowUserId(followUserId);
        follow.setCreateTime(LocalDateTime.now());
        userFollowMapper.insert(follow);
        
        // 更新关注数和粉丝数
        updateFollowCount(userId, 1);
        updateFansCount(followUserId, 1);
        
        return ResponseEntity.ok(ApiResponse.success());
    }
    
    /**
     * 取消关注
     */
    @DeleteMapping("/follow/{followUserId}")
    public ResponseEntity<ApiResponse<Void>> unfollowUser(
            @PathVariable Long followUserId,
            HttpServletRequest request) {
        Long userId = getUserIdFromRequest(request);
        
        // 删除关注关系
        int deleted = userFollowMapper.deleteByUserIdAndFollowUserId(userId, followUserId);
        if (deleted > 0) {
            // 更新关注数和粉丝数
            updateFollowCount(userId, -1);
            updateFansCount(followUserId, -1);
        }
        
        return ResponseEntity.ok(ApiResponse.success());
    }
    
    /**
     * 检查是否已关注
     */
    @GetMapping("/follow/{followUserId}")
    public ResponseEntity<ApiResponse<Boolean>> isFollowing(
            @PathVariable Long followUserId,
            HttpServletRequest request) {
        Long userId = getUserIdFromRequest(request);
        UserFollow follow = userFollowMapper.selectByUserIdAndFollowUserId(userId, followUserId);
        return ResponseEntity.ok(ApiResponse.success(follow != null));
    }
    
    /**
     * 更新用户关注数
     */
    private void updateFollowCount(Long userId, int delta) {
        User user = userMapper.selectById(userId);
        if (user != null) {
            int currentCount = user.getFollowCount() != null ? user.getFollowCount() : 0;
            user.setFollowCount(Math.max(0, currentCount + delta));
            userMapper.updateById(user);
        }
    }
    
    /**
     * 更新用户粉丝数
     */
    private void updateFansCount(Long userId, int delta) {
        User user = userMapper.selectById(userId);
        if (user != null) {
            int currentCount = user.getFansCount() != null ? user.getFansCount() : 0;
            user.setFansCount(Math.max(0, currentCount + delta));
            userMapper.updateById(user);
        }
    }
    
    /**
     * 从请求中获取用户ID
     */
    private Long getUserIdFromRequest(HttpServletRequest request) {
        Object userIdObj = request.getAttribute("userId");
        if (userIdObj == null) {
            throw new BusinessException(401, "未登录或Token无效");
        }
        return (Long) userIdObj;
    }
    
    /**
     * 验证密码
     */
    private boolean verifyPassword(String inputPassword, String storedPassword, String salt) {
        String saltedPassword = inputPassword + salt;
        String hashedPassword = DigestUtils.md5DigestAsHex(saltedPassword.getBytes(StandardCharsets.UTF_8));
        return hashedPassword.equals(storedPassword);
    }
    
    /**
     * 加密密码
     */
    private String encryptPassword(String password, String salt) {
        String saltedPassword = password + salt;
        return DigestUtils.md5DigestAsHex(saltedPassword.getBytes(StandardCharsets.UTF_8));
    }
}
