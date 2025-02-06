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
    public Mono<BlogPostsDTO> updateBlogPost(Long id, BlogPosts blogPost, Flux<FilePart> images    ) {
            return this.getBlogPostById(id)
                    .flatMap(blogPostsDTO -> Mono.just(mapper.dtoToBlogPosts(blogPostsDTO)))
                .flatMap(existingBlogPost -> {
                    if (!Objects.equals(id, blogPost.getId())) {
                        return Mono.error(new HttpException(HttpStatus.FORBIDDEN, "ID Does Not Match"));
                    }
                    if(!Objects.equals(existingBlogPost.getAuthorId(), blogPost.getAuthorId())) {
                        return Mono.error(new HttpException(HttpStatus.FORBIDDEN, "Author ID Does Not Match"));
                    }
                    existingBlogPost.setTitle(blogPost.getTitle());
                    existingBlogPost.setContent(blogPost.getContent());
                    // If new images are provided, handle them
                    if (images != null) {
                        return this.handleImageChanges(id, images)
                                .flatMap(updatedImages -> blogPostsRepository.save(existingBlogPost)
                                        .flatMap(savedBlogPost -> {
                                            BlogPostsDTO dto = mapper.blogPostsToBlogPostsDTO(savedBlogPost);
                                            dto.setImages(updatedImages);
                                            return Mono.just(dto);
                                        }));
                    }

                    return blogPostsRepository.save(existingBlogPost)
                            .flatMap(savedBlogPost -> {
                                BlogPostsDTO dto = mapper.blogPostsToBlogPostsDTO(savedBlogPost);
                                return Mono.just(dto);
                            });
                });
    }

    private Mono<List<BlogImages>> handleImageChanges(Long blogPostId, Flux<FilePart> images) {
        Mono<BlogPostsDTO> existingBlogPostMono = blogPostsRepository.findById(blogPostId)
                .flatMap(blog -> {
                    BlogPostsDTO dto = mapper.blogPostsToBlogPostsDTO(blog);
                    return imagesRepository.findByBlogPostId(blogPostId)
                            .collectList()
                            .map(image -> {
                                dto.setImages(image);
                                return dto;
                            });
                });
        // Find the existing images for the blog post
        Flux<BlogImages> existingImage = imagesRepository.findByBlogPostId(blogPostId);

        Mono<Set<String>> existingImageKey = existingImage.map(BlogImages::getImageKey)
                .collect(Collectors.toSet());

        // Filter out images that are already saved (i.e., they have existing image keys)
        Flux<FilePart> newImages = images.filterWhen(image -> existingImageKey
                .map(keys -> !keys.contains(image.filename())));

        // Find the removed images (those that are in the DB but not in the updated form)
        Mono<Set<String>> newImageKeys = images
                .map(FilePart::filename) // Get the filenames of new images
                .collect(Collectors.toSet()); // Collect them into a Set asynchronously

        // You need to wait for `existingImageKey` to resolve asynchronously in a non-blocking way
        return existingImageKey
                .zipWith(newImageKeys) // Combine both sets (existing and new images)
                .flatMap(tuple -> {
                    Set<String> existingKeys = tuple.getT1(); // Extract existing image keys
                    Set<String> newKeys = tuple.getT2(); // Extract new image keys

                    // Find removed image keys (those that are in the DB but not in the new form)
                    Set<String> removedImageKeys = existingKeys.stream()
                            .filter(key -> !newKeys.contains(key))
                            .collect(Collectors.toSet());

                    // Delete removed images from the database and the storage
                    return deleteRemovedImages(removedImageKeys)
                            .then(saveImage(newImages, blogPostId).collectList()) // Save the new images
                            .flatMap(savedImages -> existingBlogPostMono.map(existingBlogPost -> {
                                List<BlogImages> allImages = existingBlogPost.getImages().stream()
                                        .filter(image -> !removedImageKeys.contains(image.getImageKey()))
                                        .collect(Collectors.toList());
                                return allImages;
                            }));
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
