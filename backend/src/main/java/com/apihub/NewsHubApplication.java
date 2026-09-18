package com.apihub;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableCaching        // turns on @Cacheable, used by NewsService
@EnableJpaAuditing    // populates @CreatedDate / @LastModifiedDate on entities
public class NewsHubApplication {
    public static void main(String[] args) {
        SpringApplication.run(NewsHubApplication.class, args);
    }
}
