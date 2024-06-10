package dev.rickcloudy.restapi.repository.impl;

import dev.rickcloudy.restapi.entity.BlogImages;
import dev.rickcloudy.restapi.repository.CustomBlogImageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.relational.core.query.Query;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static org.springframework.data.relational.core.query.Criteria.where;

@Component
@RequiredArgsConstructor
public class CustomBlogImageRepositoryImpl implements CustomBlogImageRepository {
    private final R2dbcEntityTemplate template;
    @Override
    public Mono<Void> deleteByBlogPostId(Long blogPostId) {
        return template.delete(BlogImages.class)
                .from("blog_images")
                .matching(Query.query(where("blog_post_id").is(blogPostId)))
                .all().then();
    }

    @Override
    public Flux<BlogImages> findByBlogPostId(Long blogPostId) {
        return template.select(BlogImages.class)
                .from("blog_images")
                .matching(Query.query(where("blog_post_id").is(blogPostId)))
                .all();
    }
}
