package dev.rickcloudy.restapi.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "rickcloudy")
@Getter
@Setter
public class RickCloudyProperties {
    private Blog blog = new Blog();
    private Token token = new Token();

    @Getter
    @Setter
    public static class Blog {
        private String blogImagesBucket;
    }

    @Getter
    @Setter
    public static class Token {
        private String accessTokenSecret;
        private String refreshTokenSecret;
        private Long accessTokenExpirationMs;
        private Long refreshTokenExpirationMs;
    }
}
