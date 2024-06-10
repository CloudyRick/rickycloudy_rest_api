package dev.rickcloudy.restapi.repository;

import dev.rickcloudy.restapi.entity.BlogPosts;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BlogPostsRepository extends ReactiveCrudRepository<BlogPosts, Long>, CustomBlogRepository {

}
