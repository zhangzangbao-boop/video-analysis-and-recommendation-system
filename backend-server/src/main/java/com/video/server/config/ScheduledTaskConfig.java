package com.video.server.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 定时任务配置
 * - 离线分析：每天凌晨2:00执行一次
 * - ALS模型训练：每周一凌晨3:00执行一次
 */
@Slf4j
@Configuration
@EnableScheduling
public class ScheduledTaskConfig {

    private static final String JAR_NAME = "bigdata-engine-1.0-jar-with-dependencies.jar";
    private static final String OFFLINE_JOB_CLASS = "com.shortvideo.recommendation.offline.job.OfflineJob";
    private static final String ALS_TRAINER_CLASS = "com.shortvideo.recommendation.als.ALSTrainer";
    
    @Value("${spark.home:}")
    private String sparkHomeConfig;
    
    private final ExecutorService executorService = Executors.newFixedThreadPool(2);
    
    /**
     * 查找bigdata-engine目录
     * 从当前目录（backend-server）向上查找项目根目录，然后查找bigdata-engine
     */
    private File findBigdataEngineDir() {
        // 获取当前工作目录
        File currentDir = new File(System.getProperty("user.dir"));
        
        // 方法1: 检查当前目录下是否有bigdata-engine
        File bigdataEngineDir = new File(currentDir, "bigdata-engine");
        if (bigdataEngineDir.exists() && bigdataEngineDir.isDirectory()) {
            return bigdataEngineDir;
        }
        
        // 方法2: 检查父目录（项目根目录）下是否有bigdata-engine
        File parentDir = currentDir.getParentFile();
        if (parentDir != null) {
            bigdataEngineDir = new File(parentDir, "bigdata-engine");
            if (bigdataEngineDir.exists() && bigdataEngineDir.isDirectory()) {
                return bigdataEngineDir;
            }
        }
        
        // 方法3: 检查当前目录的父目录的父目录（如果backend-server在子目录中）
        if (parentDir != null) {
            File grandParentDir = parentDir.getParentFile();
            if (grandParentDir != null) {
                bigdataEngineDir = new File(grandParentDir, "bigdata-engine");
                if (bigdataEngineDir.exists() && bigdataEngineDir.isDirectory()) {
                    return bigdataEngineDir;
                }
            }
        }
        
        // 如果都找不到，返回默认路径（相对于当前目录）
        return new File(currentDir, "bigdata-engine");
    }

    /**
     * 离线分析任务 - 每天凌晨2:00执行
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void runOfflineJob() {
        log.info("========== 开始执行离线分析任务（定时任务） ==========");
        triggerOfflineJob(null);
    }

    /**
     * ALS模型训练任务 - 每周一凌晨3:00执行
     */
    @Scheduled(cron = "0 0 3 ? * MON")
    public void runALSTrainer() {
        log.info("========== 开始执行ALS模型训练任务（定时任务） ==========");
        triggerALSTrainer();
    }
    
    /**
     * 手动触发离线分析任务（供Controller调用）
     * @param date 可选，指定统计日期（格式：yyyy-MM-dd），不传则使用昨天
     */
    public void triggerOfflineJob(String date) {
        log.info("========== 开始执行离线分析任务 ==========");
        String dateArg = date != null && !date.isEmpty() ? date : 
            LocalDate.now().minusDays(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        executeSparkJob(OFFLINE_JOB_CLASS, "离线分析", dateArg);
    }
    
    /**
     * 手动触发ALS模型训练任务（供Controller调用）
     */
    public void triggerALSTrainer() {
        log.info("========== 开始执行ALS模型训练任务 ==========");
        executeSparkJob(ALS_TRAINER_CLASS, "ALS模型训练", null);
    }

    /**
     * 执行Spark任务
     * @param mainClass 主类名
     * @param jobName 任务名称
     * @param dateArg 可选，日期参数（用于离线分析任务）
     */
    private void executeSparkJob(String mainClass, String jobName, String dateArg) {
        executorService.submit(() -> {
            try {
                // 查找bigdata-engine目录
                File bigdataEngineDir = findBigdataEngineDir();
                log.info("{}任务 - bigdata-engine目录: {}", jobName, bigdataEngineDir.getAbsolutePath());
                
                // 检查JAR文件是否存在
                File jarFile = new File(bigdataEngineDir, "target/" + JAR_NAME);
                if (!jarFile.exists()) {
                    log.error("{}任务JAR文件不存在: {}", jobName, jarFile.getAbsolutePath());
                    log.error("请先执行: cd {} && mvn clean package -DskipTests", bigdataEngineDir.getAbsolutePath());
                    return;
                }
                
                log.info("{}任务 - 找到JAR文件: {}", jobName, jarFile.getAbsolutePath());

                // 获取Spark提交命令
                String sparkSubmitCmd = getSparkSubmitCommand(jarFile, mainClass, dateArg);
                
                log.info("{}任务执行命令: {}", jobName, sparkSubmitCmd);
                
                ProcessBuilder processBuilder = new ProcessBuilder();
                processBuilder.directory(bigdataEngineDir);
                
                // Windows环境
                if (System.getProperty("os.name").toLowerCase().contains("windows")) {
                    processBuilder.command("cmd", "/c", sparkSubmitCmd);
                } else {
                    // Linux/Mac环境
                    processBuilder.command("sh", "-c", sparkSubmitCmd);
                }
                
                processBuilder.redirectErrorStream(true);
                Process process = processBuilder.start();
                
                // 读取输出日志
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream(), "UTF-8"))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        log.info("[{}] {}", jobName, line);
                    }
                }
                
                int exitCode = process.waitFor();
                if (exitCode != 0) {
                    log.error("{}任务异常退出，退出码: {}", jobName, exitCode);
                } else {
                    log.info("{}任务执行完成", jobName);
                }
                
            } catch (Exception e) {
                log.error("执行{}任务时发生异常", jobName, e);
            }
        });
    }

    /**
     * 查找spark-submit可执行文件
     */
    private String findSparkSubmit() {
        boolean isWindows = System.getProperty("os.name").toLowerCase().contains("windows");
        String separator = isWindows ? "\\" : "/";
        String sparkSubmitCmd = isWindows ? "spark-submit.cmd" : "spark-submit";
        
        // 方法1: 检查配置文件中的spark.home
        if (sparkHomeConfig != null && !sparkHomeConfig.trim().isEmpty()) {
            String sparkSubmitPath = sparkHomeConfig.trim() + separator + "bin" + separator + sparkSubmitCmd;
            File sparkSubmitFile = new File(sparkSubmitPath);
            if (sparkSubmitFile.exists()) {
                log.info("从配置文件找到spark-submit: {}", sparkSubmitPath);
                return sparkSubmitPath;
            } else {
                log.warn("配置的spark.home路径不存在: {}", sparkSubmitPath);
            }
        }
        
        // 方法2: 检查SPARK_HOME环境变量
        String sparkHome = System.getenv("SPARK_HOME");
        if (sparkHome != null && !sparkHome.isEmpty()) {
            String sparkSubmitPath = sparkHome + separator + "bin" + separator + sparkSubmitCmd;
            File sparkSubmitFile = new File(sparkSubmitPath);
            if (sparkSubmitFile.exists()) {
                log.info("从SPARK_HOME环境变量找到spark-submit: {}", sparkSubmitPath);
                return sparkSubmitPath;
            }
        }
        
        // 方法3: 尝试常见的Spark安装路径（Windows）
        if (isWindows) {
            String[] commonPaths = {
                "C:\\spark\\bin\\spark-submit.cmd",
                "D:\\spark\\bin\\spark-submit.cmd",
                "C:\\spark-3.5.0\\bin\\spark-submit.cmd",
                "D:\\spark-3.5.0\\bin\\spark-submit.cmd",
                "C:\\Program Files\\Spark\\bin\\spark-submit.cmd",
                "D:\\Program Files\\Spark\\bin\\spark-submit.cmd"
            };
            for (String path : commonPaths) {
                File sparkSubmitFile = new File(path);
                if (sparkSubmitFile.exists()) {
                    log.info("从常见路径找到spark-submit: {}", path);
                    return path;
                }
            }
        }
        
        // 方法4: 尝试使用PATH中的spark-submit
        String sparkSubmit = isWindows ? "spark-submit.cmd" : "spark-submit";
        log.error("未找到spark-submit！");
        log.error("请选择以下方式之一：");
        log.error("1. 在 application.yml 中配置: spark.home=D:\\spark-3.5.0");
        log.error("2. 设置 SPARK_HOME 环境变量");
        log.error("3. 确保 spark-submit.cmd 在系统 PATH 中");
        log.error("将尝试使用PATH中的spark-submit: {}（可能会失败）", sparkSubmit);
        return sparkSubmit;
    }

    /**
     * 获取Spark提交命令
     * @param jarFile JAR文件
     * @param mainClass 主类名
     * @param dateArg 可选，日期参数（用于离线分析任务）
     * @return Spark提交命令
     */
    private String getSparkSubmitCommand(File jarFile, String mainClass, String dateArg) {
        String jarPath = "target/" + JAR_NAME;
        
        // 查找spark-submit可执行文件
        String sparkSubmit = findSparkSubmit();
        
        // 对于离线分析任务，传递日期参数
        String finalDateArg = "";
        if (OFFLINE_JOB_CLASS.equals(mainClass)) {
            if (dateArg != null && !dateArg.isEmpty()) {
                finalDateArg = " " + dateArg;
            } else {
                // 默认使用昨天的日期
                LocalDate yesterday = LocalDate.now().minusDays(1);
                finalDateArg = " " + yesterday.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            }
        }
        
        return String.format(
            "%s --class %s --master local[*] --driver-memory 2g --executor-memory 2g %s%s",
            sparkSubmit, mainClass, jarPath, finalDateArg
        );
    }
}
