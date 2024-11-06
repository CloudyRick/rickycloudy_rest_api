package dev.rickcloudy.restapi.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "rickcloudy")
public class RickCloudyProperties {
    private Blog blog = new Blog();

    public Blog getBlog() {
        return blog;
    }

    public static class Blog {
        private String blogImagesBucket;
        public String getBlogImagesBucket() {
            return blogImagesBucket;
        }
        public void setBlogImagesBucket(String blogImagesBucket) {
            this.blogImagesBucket = blogImagesBucket;
        }
    }
}
