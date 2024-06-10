package dev.rickcloudy.restapi.mapper;

import dev.rickcloudy.restapi.dto.BlogPostsDTO;
import dev.rickcloudy.restapi.entity.BlogPosts;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface BlogPostMapper {
    BlogPostMapper INSTANCE = Mappers.getMapper(BlogPostMapper.class);
    BlogPostsDTO blogPostsToBlogPostsDTO(BlogPosts blogPosts);
    BlogPosts dtoToBlogPosts(BlogPostsDTO blogPostsDTO);
}
