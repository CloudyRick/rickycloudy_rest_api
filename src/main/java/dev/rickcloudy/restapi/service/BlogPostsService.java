package dev.rickcloudy.restapi.service;

import dev.rickcloudy.restapi.dto.BlogPostsDTO;
import dev.rickcloudy.restapi.entity.BlogImages;
import dev.rickcloudy.restapi.entity.BlogPosts;
import dev.rickcloudy.restapi.enums.BlogStatus;
import dev.rickcloudy.restapi.exception.custom.BlogPostNotFoundException;
import dev.rickcloudy.restapi.exception.HttpException;
import dev.rickcloudy.restapi.exception.custom.NotFoundException;
import dev.rickcloudy.restapi.exception.custom.UnauthorizedException;
import dev.rickcloudy.restapi.exception.custom.UserNotFoundException;
import dev.rickcloudy.restapi.helper.ReactiveLogger;
import dev.rickcloudy.restapi.mapper.BlogPostMapper;
import dev.rickcloudy.restapi.repository.BlogImagesRepository;
import dev.rickcloudy.restapi.repository.BlogPostsRepository;
import dev.rickcloudy.restapi.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static net.sf.jsqlparser.util.validation.metadata.NamedObject.user;

@Service
@RequiredArgsConstructor
public class BlogPostsService {
    private static Logger log = LogManager.getLogger(BlogPostsService.class);
    private final BlogPostsRepository blogPostsRepository;
    private final UserRepository userRepository;
    private final BlogImagesRepository imagesRepository;
    private final BlogPostMapper mapper;
    private final S3Service s3Service;
    private final Validator validator;

    @Transactional
    public Mono<BlogPostsDTO> createBlogPost(BlogPosts blogPost, Flux<String> imagesUrl) {
        // Set up the Errors object
        Errors errors = new BeanPropertyBindingResult(blogPost, "blogPost");

        // Validate the entity
        validator.validate(blogPost, errors);

        // Check if there are any validation errors
        if (errors.hasErrors()) {
            StringBuilder errorMessage = new StringBuilder("Validation failed: ");
            errors.getAllErrors().forEach(error -> errorMessage.append(error.getDefaultMessage()).append(" "));
            return Mono.error(new HttpException(HttpStatus.BAD_REQUEST, errorMessage.toString()));
        }
        return userRepository.findById(blogPost.getAuthorId())
                .switchIfEmpty(Mono.error(new UserNotFoundException(HttpStatus.NOT_FOUND, "User Not Found")))
                .flatMap(user -> blogPostsRepository.save(blogPost))
                .flatMap(savedBlogPost -> {
                    return imagesUrl.flatMap(url -> {
                                log.debug("IMAGE URL {}", url);
                                return imagesRepository.findByUrl(url);
                            })
                            .flatMap(blogImage -> {
                                blogImage.setBlogPostId(savedBlogPost.getId());
                                return imagesRepository.save(blogImage);
                            })
                            .collectList()
                            .flatMap(images -> {
                                BlogPostsDTO dto = mapper.blogPostsToBlogPostsDTO(savedBlogPost);
                                dto.setImages(images);
                                return Mono.just(dto);
                            });
                })
                        .doOnError(err -> {
                            // Delete the uploaded images if an error occurs
                            log.error("Error saving blog post: {}", err);
                        });
    }



    private Flux<BlogImages> saveImage(Flux<FilePart> image, Long blogPostId) {
        return s3Service.uploadBlogImages(image)
                .flatMap(res -> {
                    BlogImages blogImages = BlogImages.builder()
                            .blogPostId(blogPostId)
                            .imageUrl(res.getUrl())
                            .imageKey(res.getKey())
                            .build();
                    return imagesRepository.save(blogImages);
                })
                .doOnNext(res -> log.info("Image Saved: {}", res))
                .doOnError(err -> log.error("Error Saving Image: {}", err));
    }

    public Mono<BlogPostsDTO> getBlogPostById(Long id) {
        return blogPostsRepository.findById(id)
                .switchIfEmpty(Mono.error(new BlogPostNotFoundException(HttpStatus.NOT_FOUND, "Blog Post Not Found")))
                .flatMap(res -> {
                    if (res.getStatus() == BlogStatus.DELETED) {
                        return Mono.error(new BlogPostNotFoundException(HttpStatus.NOT_FOUND, "Blog Post Not Found"));
                    }
                    if(!(res.getStatus() == BlogStatus.PUBLISHED)) {
                        return Mono.error(new UnauthorizedException("Unathorized to access blog post with id " + id));
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

    public Mono<BlogPostsDTO> getBlogPostByIdAdmin(Long id) {
        return blogPostsRepository.findById(id)
                .switchIfEmpty(Mono.error(new BlogPostNotFoundException(HttpStatus.NOT_FOUND, "Blog Post Not Found")))
                .flatMap(blogPost -> {
                    BlogPostsDTO blogPostsDTO = mapper.blogPostsToBlogPostsDTO(blogPost);
                    return imagesRepository.findByBlogPostId(blogPost.getId())
                            .collectList()
                            .doOnNext(blogPostsDTO::setImages)
                            .thenReturn(blogPostsDTO);
                });
    }

    public Flux<BlogPostsDTO> getAllBlogPostsAdmin() {
        return blogPostsRepository.findAll()
                .flatMap(blogPost -> {
                    BlogPostsDTO blogPostsDTO = mapper.blogPostsToBlogPostsDTO(blogPost);
                    return imagesRepository.findByBlogPostId(blogPost.getId())
                            .collectList()
                            .doOnNext(blogPostsDTO::setImages)
                            .thenReturn(blogPostsDTO);
                });
    }

    @Transactional
    public Mono<BlogPostsDTO> updateBlogPost(Long id, BlogPosts blogPost, Flux<String> imageUrls) {
        return blogPostsRepository.findById(id)
                .flatMap(existingBlogPost -> {
                    if (!Objects.equals(id, blogPost.getId())) {
                        return Mono.error(new HttpException(HttpStatus.FORBIDDEN, "ID Does Not Match"));
                    }
                    if (!Objects.equals(existingBlogPost.getAuthorId(), blogPost.getAuthorId())) {
                        return Mono.error(new HttpException(HttpStatus.FORBIDDEN, "Author ID Does Not Match"));
                    }
                    blogPost.setCreatedAt(existingBlogPost.getCreatedAt());
                    blogPost.setUpdatedAt(ZonedDateTime.now());
                    log.debug("Blog Post {}", blogPost);
                    // If new image URLs are provided, update the blog post with them
                    if (imageUrls != null) {
                        return this.handleImageChanges(id, imageUrls)
                                .flatMap(updatedImageUrls -> blogPostsRepository.save(blogPost)
                                        .flatMap(savedBlogPost -> {
                                            BlogPostsDTO dto = mapper.blogPostsToBlogPostsDTO(savedBlogPost);
                                            dto.setImages(updatedImageUrls);
                                            return Mono.just(dto);
                                }));
                    }

                    // If no images, just save and return
                    return blogPostsRepository.save(blogPost)
                            .flatMap(savedBlogPost -> {
                                BlogPostsDTO dto = mapper.blogPostsToBlogPostsDTO(savedBlogPost);
                                return Mono.just(dto);
                            });
                });
    }


    private Mono<List<BlogImages>> handleImageChanges(Long blogPostId, Flux<String> imageUrls) {
        // Fetch existing blog post and associated images
        Mono<BlogPostsDTO> existingBlogPostMono = blogPostsRepository.findById(blogPostId)
                .flatMap(blog -> {
                    BlogPostsDTO dto = mapper.blogPostsToBlogPostsDTO(blog);
                    return imagesRepository.findByBlogPostId(blogPostId)
                            .collectList()
                            .map(images -> {
                                dto.setImages(images);
                                return dto;
                            });
                });

        // Retrieve existing images from the database
        Flux<BlogImages> existingImages = imagesRepository.findByBlogPostId(blogPostId);
        Mono<Set<String>> existingImageKeys = existingImages
                .map(BlogImages::getImageKey)
                .collect(Collectors.toSet());

        // Collect new image URLs into a set
        Mono<Set<String>> newImageKeys = imageUrls.collect(Collectors.toSet());

        // Determine which images need to be added and removed
        return existingImageKeys.zipWith(newImageKeys).flatMap(tuple -> {
            Set<String> existingKeys = tuple.getT1(); // Image keys currently in DB
            Set<String> newKeys = tuple.getT2(); // Image keys provided in the update

            // Identify images to remove (present in DB but missing from the updated list)
            Set<String> removedImageKeys = existingKeys.stream()
                    .filter(key -> !newKeys.contains(key))
                    .collect(Collectors.toSet());

            // Identify new images to add (present in update but not in DB)
            Set<String> newImagesToAdd = newKeys.stream()
                    .filter(key -> !existingKeys.contains(key))
                    .collect(Collectors.toSet());

            // Remove images that are no longer needed
            return deleteRemovedImages(removedImageKeys)
                    .thenMany(setImageBlogId(newImagesToAdd, blogPostId)) // Save new images
                    .collectList()
                    .flatMap(savedImages -> existingBlogPostMono.map(existingBlogPost -> {
                        List<BlogImages> allImages = existingBlogPost.getImages().stream()
                                .filter(image -> !removedImageKeys.contains(image.getImageKey()))
                                .collect(Collectors.toList());

                        // Add newly saved images to the list
                        allImages.addAll(savedImages);
                        return allImages;
                    }));
        });
    }

    private Flux<BlogImages> setImageBlogId(Set<String> imageUrls, Long blogPostId) {
        return Flux.fromIterable(imageUrls)
                .flatMap(imagesRepository::findByUrl)
                .flatMap(img -> {
                    img.setBlogPostId(blogPostId);
                    return imagesRepository.save(img);
                });
    }


    private Mono<Void>  deleteRemovedImages(Set<String> removedImageKeys) {
        if (removedImageKeys.isEmpty()) {
            return Mono.empty(); // No images to delete
        }
        return imagesRepository.findAllByImageKeys(removedImageKeys)
                .flatMap(blogImages -> {
                    return s3Service.deleteRickCloudyBlogImage(blogImages.getImageKey())
                            .then(imagesRepository.delete(blogImages));
                })
                .then();
    }

    public Mono<Void> deleteBlogPost(Long id) {
        return this.getBlogPostById(id)
                .flatMap(dto -> {
                    return Mono.just(mapper.dtoToBlogPosts(dto));

                })
                .flatMap(blogPost -> {
                    blogPost.setStatus(BlogStatus.DELETED);
                    return blogPostsRepository.save(blogPost).then();
                });
    }

    public Flux<BlogPostsDTO> findByQueryParam(Map<String, String> param) {
        BlogStatus status = BlogStatus.fromString(param.getOrDefault("status", "PUBLISHED")); // Default to PUBLISHED

        if (status != BlogStatus.PUBLISHED) {
            return Flux.error(new UnauthorizedException("Authentication required for non-published blogs"));
        }

        return blogPostsRepository.findByParams(param)
                .filter(blog -> blog.getStatus() == BlogStatus.PUBLISHED) // Filter while streaming
                .flatMap(blog -> imagesRepository.findByBlogPostId(blog.getId())
                        .collectList()
                        .map(images -> {
                            BlogPostsDTO dto = mapper.blogPostsToBlogPostsDTO(blog);
                            dto.setImages(images);
                            return dto;
                        })
                        .doOnSuccess(res -> log.debug("Result : {} ", res))
                )
                .switchIfEmpty(Mono.error(new NotFoundException("No published blog posts found")));
    }


}
