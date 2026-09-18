package com.apihub.config;

import java.time.Duration;
import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

/**
 * Timeouts on every outbound call.
 *
 * WHY this matters: without them a hung news provider would hold a Tomcat
 * worker thread indefinitely and eventually take the whole API down. The old
 * Node backend had no timeouts at all.
 *
 * SimpleClientHttpRequestFactory (JDK HttpURLConnection) is used deliberately:
 * it is part of core Spring Framework and needs no extra HTTP-client
 * dependency. Swap in Apache HttpClient later if you need connection pooling.
 */
@Configuration
public class RestClientConfig {

    @Bean
    public RestClientCustomizer restClientCustomizer() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(10));

        return builder -> builder.requestFactory(factory);
    }
}
