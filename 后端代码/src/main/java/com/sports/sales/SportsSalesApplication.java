package com.sports.sales;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@MapperScan("com.sports.sales.mapper")
@EnableCaching
@EnableAsync
@EnableScheduling
@EnableTransactionManagement
public class SportsSalesApplication {

    public static void main(String[] args) {
        SpringApplication.run(SportsSalesApplication.class, args);
    }
}
