package com.example.javaea;

import org.jspecify.annotations.NonNull;
import org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TomcatConfig {

    @Bean
    public WebServerFactoryCustomizer<@NonNull TomcatServletWebServerFactory> tomcatCustomizer() {
        return factory -> factory.addConnectorCustomizers(connector -> {
            connector.setMaxLocaleCount(50);
            System.out.println(">>> Tomcat maxLocaleCount set to: " + connector.getMaxLocaleCount());
        });
    }
}