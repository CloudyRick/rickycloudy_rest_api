package dev.rickcloudy.restapi.repository;

import dev.rickcloudy.restapi.entity.BlogImages;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Set;

public interface CustomBlogImageRepository {
    Mono<Void> deleteByBlogPostId(Long blogPostId);
    Flux<BlogImages> findByBlogPostId(Long blogPostId);
    Flux<BlogImages> findAllByImageKeys(Set<String> imageKeys);
    Mono<BlogImages> findByUrl(String url);
}
