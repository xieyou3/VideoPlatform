package com.videoplatform.video;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.sql.init.SqlInitializationAutoConfiguration;

@SpringBootApplication(
    scanBasePackages = "com.videoplatform",
    exclude = {SqlInitializationAutoConfiguration.class}
)
@MapperScan("com.videoplatform.video.mapper")
public class VideoApplication {

    public static void main(String[] args) {
        SpringApplication.run(VideoApplication.class, args);
    }
}
