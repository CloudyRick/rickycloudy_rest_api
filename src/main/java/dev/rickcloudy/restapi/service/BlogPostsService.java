package dev.rickcloudy.restapi.service;

import dev.rickcloudy.restapi.dto.BlogPostsDTO;
import dev.rickcloudy.restapi.entity.BlogImages;
import dev.rickcloudy.restapi.entity.BlogPosts;
import dev.rickcloudy.restapi.enums.BlogStatus;
import dev.rickcloudy.restapi.exception.BlogPostNotFoundException;
import dev.rickcloudy.restapi.exception.HttpException;
import dev.rickcloudy.restapi.exception.UserNotFoundException;
import dev.rickcloudy.restapi.mapper.BlogPostMapper;
import dev.rickcloudy.restapi.repository.BlogImagesRepository;
import dev.rickcloudy.restapi.repository.BlogPostsRepository;
import dev.rickcloudy.restapi.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class BlogPostsService {
    private static Logger log = LogManager.getLogger(BlogPostsService.class);
    private final BlogPostsRepository blogPostsRepository;
    private final UserRepository userRepository;
    private final BlogImagesRepository imagesRepository;
    private final BlogPostMapper mapper;
    private final S3Service s3Service;

    @Transactional
    public Mono<BlogPostsDTO> createBlogPost(BlogPosts blogPost, Flux<FilePart> images) {
        return userRepository.findById(blogPost.getAuthorId())
                .switchIfEmpty(Mono.error(new UserNotFoundException(HttpStatus.NOT_FOUND, "User Not Found")))
                .flatMap(user -> blogPostsRepository.save(blogPost))
                .flatMap(savedBlogPost -> {
                            return saveImage(images, savedBlogPost.getId())
                                    .collectList()
                                    .map(savedImages -> {
                                        BlogPostsDTO dto = mapper.blogPostsToBlogPostsDTO(savedBlogPost);
                                        dto.setImages(savedImages);
                                        return dto;
                                    });
                        });
    }

    private Flux<BlogImages> saveImage(Flux<FilePart> image, Long blogPostId) {
        return s3Service.uploadBlogImages(image)
                .flatMap(res -> {
                    BlogImages blogImages = BlogImages.builder()
                            .blogPostId(blogPostId)
                            .imageUrl(res.getUrl())
                            .build();
                    return imagesRepository.save(blogImages);
                })
                .doOnNext(res -> log.info("Image Saved: {}", res))
                .doOnError(err -> log.error("Error Saving Image: {}", err));
    }

    public Mono<BlogPostsDTO> getBlogPost(Long id) {
        return blogPostsRepository.findById(id)
                .switchIfEmpty(Mono.error(new BlogPostNotFoundException(HttpStatus.NOT_FOUND, "Blog Post Not Found")))
                .flatMap(res -> {
                    if (res.getStatus() == BlogStatus.DELETED) {
                        return Mono.error(new BlogPostNotFoundException(HttpStatus.NOT_FOUND, "Blog Post Not Found"));
                    }
                    return Mono.just(res);
                })
                .flatMap(blogPost -> {
                    BlogPostsDTO blogPostsDTO = mapper.blogPostsToBlogPostsDTO(blogPost);
                    return imagesRepository.findByBlogPostId(blogPost.getId())
                            .collectList()
                            .doOnNext(blogPostsDTO::setImages)
                            .thenReturn(blogPostsDTO);
                });
    }

    public Flux<BlogPosts> getAllBlogPosts() {
        return blogPostsRepository.findAll()
                .flatMap(blogPost -> {
                    if (blogPost.getStatus() == BlogStatus.DELETED) {
                        return Mono.empty();
                    }
                    return Mono.just(blogPost);
                })
                .flatMap(blogPost -> {
                    BlogPostsDTO blogPostsDTO = mapper.blogPostsToBlogPostsDTO(blogPost);
                    return imagesRepository.findByBlogPostId(blogPost.getId())
                            .collectList()
                            .doOnNext(blogPostsDTO::setImages)
                            .thenReturn(blogPost);
                });
    }

    @Transactional
    public Mono<BlogPosts> updateBlogPost(Long id, BlogPosts blogPost) {
            return this.getBlogPost(id)
                    .flatMap(blogPostsDTO -> {
                        return Mono.just(mapper.dtoToBlogPosts(blogPostsDTO));
                    })
                .flatMap(existingBlogPost -> {
                    if (!Objects.equals(id, blogPost.getId())) {
                        return Mono.error(new HttpException(HttpStatus.FORBIDDEN, "ID Does Not Match"));
                    }
                    if(!Objects.equals(existingBlogPost.getAuthorId(), blogPost.getAuthorId())) {
                        return Mono.error(new HttpException(HttpStatus.FORBIDDEN, "Author ID Does Not Match"));
                    }
                    existingBlogPost.setTitle(blogPost.getTitle());
                    existingBlogPost.setContent(blogPost.getContent());
                    return blogPostsRepository.save(existingBlogPost);
                });
    }

    public Mono<Void> deleteBlogPost(Long id) {
        return this.getBlogPost(id)
                .flatMap(dto -> {
                    return Mono.just(mapper.dtoToBlogPosts(dto));

                })
                .flatMap(blogPost -> {
                    blogPost.setStatus(BlogStatus.DELETED);
                    return blogPostsRepository.save(blogPost).then();
                });
    }

    public Flux<BlogPosts> findByQueryParam(Map<String, String> param) {
        return blogPostsRepository.findByParams(param);
    }
}
