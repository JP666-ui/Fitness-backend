package com.fitness;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
// 扫描mapper包，改成你的mapper所在路径
@MapperScan("com.fitness.mapper")
public class FitnessBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(FitnessBackendApplication.class, args);
    }

}