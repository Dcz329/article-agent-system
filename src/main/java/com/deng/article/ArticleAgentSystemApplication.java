package com.deng.article;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.deng.article.mapper")
public class ArticleAgentSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(ArticleAgentSystemApplication.class, args);
    }

}