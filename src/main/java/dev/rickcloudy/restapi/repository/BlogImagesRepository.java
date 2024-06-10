package dev.rickcloudy.restapi.repository;

import dev.rickcloudy.restapi.entity.BlogImages;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

public interface BlogImagesRepository extends ReactiveCrudRepository<BlogImages, Long>, CustomBlogImageRepository {

}
