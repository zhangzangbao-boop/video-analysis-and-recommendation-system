// 项目基本信息
name := "shortvideo-recommendation"
version := "1.0"
scalaVersion := "2.12.21"  // 必须和SBT编译时用的Scala版本一致

// 依赖仓库（优先阿里云）
resolvers ++= Seq(
  "Aliyun Maven" at "https://maven.aliyun.com/repository/public/",
  "Typesafe Repository" at "https://repo.typesafe.com/typesafe/releases/",
  "Maven Central" at "https://repo1.maven.org/maven2/"
)

// 核心依赖（Spark、Hadoop、Kafka、配置工具等）
libraryDependencies ++= Seq(
  // Spark核心依赖
  "org.apache.spark" %% "spark-core" % "2.4.8" % "provided",
  "org.apache.spark" %% "spark-sql" % "2.4.8" % "provided",
  "org.apache.spark" %% "spark-mllib" % "2.4.8" % "provided",
  "org.apache.spark" %% "spark-streaming" % "2.4.8" % "provided",
  "org.apache.spark" %% "spark-sql-kafka-0-10" % "2.4.8",

  // Hadoop依赖
  "org.apache.hadoop" % "hadoop-common" % "2.7.3",
  "org.apache.hadoop" % "hadoop-hdfs" % "2.7.3",
  "org.apache.hadoop" % "hadoop-client" % "2.7.3",

  // Kafka依赖
  "org.apache.kafka" % "kafka-clients" % "2.4.1",

  // 配置文件解析（typesafe config）
  "com.typesafe" % "config" % "1.4.2",

  // MySQL连接
  "mysql" % "mysql-connector-java" % "8.0.33",

  // 日志依赖
  "org.slf4j" % "slf4j-api" % "1.7.36",
  "ch.qos.logback" % "logback-classic" % "1.2.13"
)

// 编译选项
scalacOptions ++= Seq(
  "-target:jvm-1.8",  // 适配JDK 8
  "-deprecation",     // 显示废弃警告
  "-feature",         // 显示特性警告
  "-unchecked"        // 显示未检查类型警告
)

// JDK版本设置
javaHome := Some(file(System.getenv("JAVA_HOME")))
javacOptions ++= Seq("-source", "1.8", "-target", "1.8")