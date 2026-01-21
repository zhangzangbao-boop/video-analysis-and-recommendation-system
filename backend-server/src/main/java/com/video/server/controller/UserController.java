package com.video.server.controller;

import com.video.server.dto.ApiResponse;
import com.video.server.dto.PasswordChangeRequest;
import com.video.server.dto.UserUpdateRequest;
import com.video.server.entity.User;
import com.video.server.entity.UserFollow;
import com.video.server.dto.VideoDTO;
import com.video.server.entity.Video;
import com.video.server.entity.VideoComment;
import com.video.server.exception.BusinessException;
import com.video.server.mapper.UserFollowMapper;
import com.video.server.mapper.UserMapper;
import com.video.server.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.*;
import com.video.server.utils.TencentCosVideoUtil; // 确保引入
import org.springframework.web.multipart.MultipartFile;
import javax.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

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
    private final UserFollowMapper userFollowMapper;
    private final VideoPlayRecordService playRecordService;
    private final VideoInteractionService interactionService;
    private final VideoCommentService commentService;
    private final VideoService videoService;
    private final TencentCosVideoUtil tencentCosVideoUtil;
    /**
     * 获取当前用户信息
     */
    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<User>> getCurrentUser(HttpServletRequest request) {
        Long userId = getUserIdFromRequest(request);
        User user = userService.getUserById(userId);
        if (user != null) {
            user.setPassword(null);
            user.setSalt(null);
        }
        return ResponseEntity.ok(ApiResponse.success(user));
    }

    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<User>> updateProfile(@RequestBody UserUpdateRequest request, HttpServletRequest httpRequest) {
        Long userId = getUserIdFromRequest(httpRequest);
        User user = userService.getUserById(userId);
        if (request.getNickname() != null) user.setNickname(request.getNickname());
        if (request.getAvatarUrl() != null) user.setAvatarUrl(request.getAvatarUrl());
        if (request.getRealName() != null) user.setRealName(request.getRealName());
        if (request.getEmail() != null) user.setEmail(request.getEmail());
        if (request.getGender() != null) {
            if (request.getGender() == 1) {
                user.setGender("male");
            } else if (request.getGender() == 0) {
                user.setGender("female");
            } else {
                user.setGender("secret"); // 或者其他默认值
            }
        }        if (request.getBio() != null) user.setBio(request.getBio());
        if (request.getPhone() != null) user.setPhone(request.getPhone());
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);
        return ResponseEntity.ok(ApiResponse.success(user));
    }

    @PutMapping("/password")
    public ResponseEntity<ApiResponse<Void>> changePassword(@RequestBody PasswordChangeRequest request, HttpServletRequest httpRequest) {
        Long userId = getUserIdFromRequest(httpRequest);
        User user = userService.getUserById(userId);
        if (!verifyPassword(request.getOldPassword(), user.getPassword(), user.getSalt())) {
            throw new BusinessException(400, "旧密码错误");
        }
        String newSalt = UUID.randomUUID().toString().replace("-", "");
        String encryptedPassword = encryptPassword(request.getNewPassword(), newSalt);
        userMapper.updatePasswordById(userId, encryptedPassword, newSalt);
        return ResponseEntity.ok(ApiResponse.success());
    }

    // 播放历史
    @GetMapping("/history")
    public ResponseEntity<ApiResponse<List<Video>>> getPlayHistory(@RequestParam(defaultValue = "1") Integer page, @RequestParam(defaultValue = "20") Integer pageSize, HttpServletRequest request) {
        Long userId = getUserIdFromRequest(request);
        List<Video> videos = playRecordService.getPlayHistory(userId, pageSize);
        return ResponseEntity.ok(ApiResponse.success(videos));
    }

    // 点赞记录
    @GetMapping("/likes")
    public ResponseEntity<ApiResponse<List<VideoDTO>>> getLikes(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize,
            HttpServletRequest request) {
        Long userId = getUserIdFromRequest(request);
        List<VideoDTO> videos = interactionService.getLikedVideos(userId, pageSize);
        return ResponseEntity.ok(ApiResponse.success(videos));
    }

    // 收藏记录
    @GetMapping("/collects")
    public ResponseEntity<ApiResponse<List<VideoDTO>>> getCollectedVideos(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize,
            HttpServletRequest request) {
        Long userId = getUserIdFromRequest(request);
        List<VideoDTO> videos = interactionService.getCollectedVideos(userId, pageSize);
        return ResponseEntity.ok(ApiResponse.success(videos));
    }

    // 评论记录
    @GetMapping("/comments")
    public ResponseEntity<ApiResponse<List<VideoComment>>> getComments(@RequestParam(defaultValue = "1") Integer page, @RequestParam(defaultValue = "20") Integer pageSize, HttpServletRequest request) {
        Long userId = getUserIdFromRequest(request);
        List<VideoComment> comments = commentService.getCommentsByUserId(userId, pageSize);
        return ResponseEntity.ok(ApiResponse.success(comments));
    }

    /**
     * 获取我的作品
     */
    @GetMapping("/works")
    public ResponseEntity<ApiResponse<List<Video>>> getMyWorks(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize,
            HttpServletRequest request) {
        Long userId = getUserIdFromRequest(request);
        List<Video> videos = videoService.getUserPublishedVideos(userId, pageSize);
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

        UserFollow existing = userFollowMapper.selectByUserIdAndFollowUserId(userId, followUserId);
        if (existing != null) {
            throw new BusinessException(400, "已经关注过该用户");
        }

        UserFollow follow = new UserFollow();
        follow.setUserId(userId);
        follow.setFollowUserId(followUserId);
        follow.setCreateTime(LocalDateTime.now());
        userFollowMapper.insert(follow);

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

        int deleted = userFollowMapper.deleteByUserIdAndFollowUserId(userId, followUserId);
        if (deleted > 0) {
            updateFollowCount(userId, -1);
            updateFansCount(followUserId, -1);
        }

        return ResponseEntity.ok(ApiResponse.success());
    }

    /**
     * 检查是否已关注
     */
    @GetMapping("/follow/{followUserId}/status")
    public ResponseEntity<ApiResponse<Boolean>> isFollowing(
            @PathVariable Long followUserId,
            HttpServletRequest request) {
        Long userId = getUserIdFromRequest(request);
        UserFollow follow = userFollowMapper.selectByUserIdAndFollowUserId(userId, followUserId);
        return ResponseEntity.ok(ApiResponse.success(follow != null));
    }

    /**
     * 获取关注列表
     */
    @GetMapping("/following")
    public ResponseEntity<ApiResponse<List<User>>> getFollowingList(HttpServletRequest request) {
        Long userId = getUserIdFromRequest(request);
        List<User> followingList = userFollowMapper.selectFollowingList(userId);
        return ResponseEntity.ok(ApiResponse.success(followingList));
    }

    /**
     * 获取粉丝列表
     */
    @GetMapping("/fans")
    public ResponseEntity<ApiResponse<List<User>>> getFansList(HttpServletRequest request) {
        Long userId = getUserIdFromRequest(request);
        List<User> fansList = userFollowMapper.selectFansList(userId);
        return ResponseEntity.ok(ApiResponse.success(fansList));
    }

    // --- 辅助方法 ---

    private void updateFollowCount(Long userId, int delta) {
        User user = userMapper.selectById(userId);
        if (user != null) {
            int currentCount = user.getFollowCount() != null ? user.getFollowCount() : 0;
            user.setFollowCount(Math.max(0, currentCount + delta));
            userMapper.updateById(user);
        }
    }

    private void updateFansCount(Long userId, int delta) {
        User user = userMapper.selectById(userId);
        if (user != null) {
            int currentCount = user.getFansCount() != null ? user.getFansCount() : 0;
            user.setFansCount(Math.max(0, currentCount + delta));
            userMapper.updateById(user);
        }
    }

    private Long getUserIdFromRequest(HttpServletRequest request) {
        Object userIdObj = request.getAttribute("userId");
        if (userIdObj == null) {
            throw new BusinessException(401, "未登录或Token无效");
        }
        return (Long) userIdObj;
    }

    private boolean verifyPassword(String inputPassword, String storedPassword, String salt) {
        String saltedPassword = inputPassword + salt;
        String hashedPassword = DigestUtils.md5DigestAsHex(saltedPassword.getBytes(StandardCharsets.UTF_8));
        return hashedPassword.equals(storedPassword);
    }

    private String encryptPassword(String password, String salt) {
        String saltedPassword = password + salt;
        return DigestUtils.md5DigestAsHex(saltedPassword.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 【新增】删除单条播放历史
     */
    @DeleteMapping("/history/{videoId}")
    public ResponseEntity<ApiResponse<Void>> deletePlayHistory(@PathVariable Long videoId, HttpServletRequest request) {
        Long userId = getUserIdFromRequest(request);
        playRecordService.deleteRecord(userId, videoId);
        return ResponseEntity.ok(ApiResponse.success());
    }

    /**
     * 【新增】清空播放历史
     */
    @DeleteMapping("/history")
    public ResponseEntity<ApiResponse<Void>> clearPlayHistory(HttpServletRequest request) {
        Long userId = getUserIdFromRequest(request);
        playRecordService.clearHistory(userId);
        return ResponseEntity.ok(ApiResponse.success());
    }

    /**
     * 【新增】上传图片文件 (头像/封面)
     */
    @PostMapping("/upload/file")
    public ResponseEntity<ApiResponse<String>> uploadFile(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            throw new BusinessException(400, "文件不能为空");
        }
        // 复用封面上传逻辑，或者您可以写一个 uploadImage 通用方法
        String url = tencentCosVideoUtil.uploadCover(file);
        return ResponseEntity.ok(ApiResponse.success(url));
    }
}