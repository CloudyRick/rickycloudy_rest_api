package dev.rickcloudy.restapi.repository;

import dev.rickcloudy.restapi.entity.BlogPosts;
import reactor.core.publisher.Flux;

import java.util.Map;

public interface CustomBlogRepository {
    Flux<BlogPosts> findByParams(Map<String, String> params);
}
