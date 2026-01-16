package com.video.server.config;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.region.Region;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 腾讯云COS配置类 - 新增，和RedisConfig同级
 * Spring容器单例管理COS客户端，全局只初始化一次
 */
@Configuration
public class TencentCosConfig {

    @Value("${tencent.cos.secret-id}")
    private String secretId;

    @Value("${tencent.cos.secret-key}")
    private String secretKey;

    @Value("${tencent.cos.region}")
    private String region;

    /**
     * 初始化COS客户端，注入Spring容器
     */
    @Bean
    public COSClient cosClient() {
        // 1. 初始化密钥信息
        COSCredentials credentials = new BasicCOSCredentials(secretId, secretKey);
        // 2. 设置存储桶地域
        ClientConfig clientConfig = new ClientConfig(new Region(region));
        // 3. 创建客户端，返回即可
        return new COSClient(credentials, clientConfig);
    }
}