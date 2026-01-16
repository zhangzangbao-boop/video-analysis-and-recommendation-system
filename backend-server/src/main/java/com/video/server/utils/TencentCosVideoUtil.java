package com.video.server.utils;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.CannedAccessControlList;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.model.PutObjectResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

/**
 * 腾讯云COS工具类 - 短视频专属
 * 封装：视频上传、封面上传、视频删除、封面删除、获取播放链接等方法
 * 注意：COS客户端为可选依赖，如果未配置COS，相关方法将抛出异常
 */
@Component
@Slf4j
public class TencentCosVideoUtil {

    @Autowired(required = false)
    private COSClient cosClient;

    // ========== 从yml读取配置 ==========
    @Value("${tencent.cos.bucket-name:}")
    private String bucketName;

    @Value("${tencent.cos.domain:}")
    private String domain;

    @Value("${tencent.cos.access-control:PublicRead}")
    private String accessControl;

    @Value("${tencent.cos.video-folder:video/}")
    private String videoFolder;

    @Value("${tencent.cos.cover-folder:cover/}")
    private String coverFolder;

    /**
     * 核心方法1：上传短视频文件（前端上传的视频，调用这个方法）
     * @param file 前端上传的视频文件 MultipartFile
     * @return 腾讯云COS的视频完整播放URL (可直接在前端播放，如video标签的src)
     */
    public String uploadVideo(MultipartFile file) {
        return uploadFile(file, videoFolder);
    }

    /**
     * 核心方法2：上传视频封面图片（前端上传的封面，调用这个方法）
     * @param file 前端上传的封面图片 MultipartFile
     * @return 腾讯云COS的封面完整访问URL
     */
    public String uploadCover(MultipartFile file) {
        return uploadFile(file, coverFolder);
    }

    /**
     * 公共上传方法 - 内部调用，封装视频/封面的共同上传逻辑
     */
    private String uploadFile(MultipartFile file, String folderPath) {
        if (cosClient == null || bucketName == null || bucketName.isEmpty()) {
            log.error("COS客户端未配置，无法上传文件");
            throw new RuntimeException("文件上传服务未配置，请联系管理员");
        }
        try {
            // 判空
            if (file.isEmpty()) {
                log.error("COS文件上传失败：文件为空");
                throw new RuntimeException("上传文件不能为空");
            }
            // 获取原文件后缀 (如 .mp4 .avi .jpg .png)
            String originalFilename = file.getOriginalFilename();
            String suffix = originalFilename.substring(originalFilename.lastIndexOf("."));
            // 生成唯一文件名：UUID + 后缀，防止重名覆盖
            String fileName = UUID.randomUUID().toString().replace("-", "") + suffix;
            // 拼接COS中的文件全路径
            String cosFilePath = folderPath + fileName;

            // 获取文件输入流
            InputStream inputStream = file.getInputStream();
            // 构建上传请求
            PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, cosFilePath, inputStream, null);
            // 设置文件权限：公有读私有写
            putObjectRequest.setCannedAcl(CannedAccessControlList.valueOf(accessControl));
            // 执行上传
            PutObjectResult result = cosClient.putObject(putObjectRequest);
            log.info("COS文件上传成功，文件路径：{}，ETag：{}", cosFilePath, result.getETag());

            // 返回完整的访问URL（前端直接用这个URL播放视频/展示封面）
            return domain + "/" + cosFilePath;
        } catch (IOException e) {
            log.error("COS文件上传失败", e);
            throw new RuntimeException("文件上传至腾讯云失败，请稍后重试");
        }
    }

    /**
     * 核心方法3：删除COS上的文件（删除视频/封面通用）
     * @param cosFileUrl 视频/封面的完整URL（如：https://xxx.cos.ap-guangzhou.myqcloud.com/video/short_video/xxx.mp4）
     * @return true-删除成功 false-删除失败
     */
    public boolean deleteFile(String cosFileUrl) {
        if (cosClient == null || bucketName == null || bucketName.isEmpty() || domain == null || domain.isEmpty()) {
            log.error("COS客户端未配置，无法删除文件");
            return false;
        }
        try {
            // 从完整URL中提取COS的文件路径（核心：截取domain后面的部分）
            String cosFilePath = cosFileUrl.substring(domain.length() + 1);
            // 执行删除
            cosClient.deleteObject(bucketName, cosFilePath);
            log.info("COS文件删除成功，文件路径：{}", cosFilePath);
            return true;
        } catch (Exception e) {
            log.error("COS文件删除失败", e);
            return false;
        }
    }

    /**
     * 核心方法4：判断文件是否存在（判断视频/封面是否存在）
     * @param cosFileUrl 视频/封面的完整URL
     * @return true-存在 false-不存在
     */
    public boolean isFileExist(String cosFileUrl) {
        if (cosClient == null || bucketName == null || bucketName.isEmpty() || domain == null || domain.isEmpty()) {
            log.error("COS客户端未配置，无法检查文件");
            return false;
        }
        try {
            String cosFilePath = cosFileUrl.substring(domain.length() + 1);
            return cosClient.doesObjectExist(bucketName, cosFilePath);
        } catch (Exception e) {
            log.error("COS文件存在性校验失败", e);
            return false;
        }
    }
}
