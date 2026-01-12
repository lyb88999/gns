package com.gns.notification;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.gns.notification.domain")
@org.springframework.scheduling.annotation.EnableScheduling
public class GeneralNotificationSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(GeneralNotificationSystemApplication.class, args);
    }
}
