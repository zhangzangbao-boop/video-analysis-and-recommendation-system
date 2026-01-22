package com.video.server.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 实时分析应用自动启动器
 * 在Spring Boot启动后自动启动实时分析应用
 */
@Slf4j
@Component
@Order(1)
public class RealtimeAnalysisStarter implements CommandLineRunner {

    private static final String JAR_NAME = "bigdata-engine-1.0-jar-with-dependencies.jar";
    private static final String MAIN_CLASS = "com.shortvideo.recommendation.realtime.app.RealtimeAnalysisApp";
    
    @Value("${spark.home:}")
    private String sparkHomeConfig;

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

    @Override
    public void run(String... args) {
        log.info("========== 开始启动实时分析应用 ==========");
        
        // 打印所有Spark相关的环境变量（用于调试）
        log.debug("检查Spark相关环境变量:");
        System.getenv().entrySet().stream()
            .filter(e -> e.getKey().toUpperCase().contains("SPARK"))
            .forEach(e -> log.debug("  {} = {}", e.getKey(), e.getValue()));
        if (sparkHomeConfig != null && !sparkHomeConfig.trim().isEmpty()) {
            log.info("配置文件中的 spark.home = {}", sparkHomeConfig);
        }
        
        try {
            // 查找bigdata-engine目录
            File bigdataEngineDir = findBigdataEngineDir();
            log.info("bigdata-engine目录: {}", bigdataEngineDir.getAbsolutePath());
            
            // 检查JAR文件是否存在
            File jarFile = new File(bigdataEngineDir, "target/" + JAR_NAME);
            if (!jarFile.exists()) {
                log.warn("实时分析应用JAR文件不存在: {}", jarFile.getAbsolutePath());
                log.warn("请先执行: cd {} && mvn clean package -DskipTests", bigdataEngineDir.getAbsolutePath());
                return;
            }
            
            log.info("找到JAR文件: {}", jarFile.getAbsolutePath());

            // 获取Spark提交命令
            String sparkSubmitCmd = getSparkSubmitCommand(jarFile);
            
            log.info("执行命令: {}", sparkSubmitCmd);
            
            // 在后台线程中启动实时分析应用
            ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "RealtimeAnalysisApp-Thread");
                t.setDaemon(true);
                return t;
            });
            
            executor.submit(() -> {
                try {
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
                            log.info("[RealtimeAnalysisApp] {}", line);
                        }
                    }
                    
                    int exitCode = process.waitFor();
                    if (exitCode != 0) {
                        log.error("实时分析应用异常退出，退出码: {}", exitCode);
                    } else {
                        log.info("实时分析应用正常退出");
                    }
                } catch (Exception e) {
                    log.error("启动实时分析应用失败", e);
                }
            });
            
            log.info("实时分析应用已在后台启动");
            
        } catch (Exception e) {
            log.error("启动实时分析应用时发生异常", e);
        }
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
     */
    private String getSparkSubmitCommand(File jarFile) {
        // 获取JAR文件的相对路径（从bigdata-engine目录）
        String jarPath = "target/" + JAR_NAME;
        
        // 查找spark-submit可执行文件
        String sparkSubmit = findSparkSubmit();
        
        return String.format(
            "%s --class %s --master local[*] --driver-memory 2g --executor-memory 2g %s",
            sparkSubmit, MAIN_CLASS, jarPath
        );
    }
}
