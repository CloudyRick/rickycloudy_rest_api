package dev.rickcloudy.restapi.repository.impl;

import dev.rickcloudy.restapi.entity.BlogImages;
import dev.rickcloudy.restapi.repository.CustomBlogImageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.core.query.Query;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Set;

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

    @Override
    public Flux<BlogImages> findAllByImageKeys(Set<String> imageKeys) {
        // Check if the set is empty and return Flux.empty() if it is
        if (imageKeys == null || imageKeys.isEmpty()) {
            return Flux.empty();
        }

        // Construct and execute the query with the image keys
        return template.select(Query.query(
                Criteria.where("image_key").in(imageKeys)
        ), BlogImages.class);
    }
}
