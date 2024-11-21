package dev.rickcloudy.restapi.service;

import dev.rickcloudy.restapi.dto.BlogPostsDTO;
import dev.rickcloudy.restapi.entity.BlogImages;
import dev.rickcloudy.restapi.entity.BlogPosts;
import dev.rickcloudy.restapi.enums.BlogStatus;
import dev.rickcloudy.restapi.exception.custom.BlogPostNotFoundException;
import dev.rickcloudy.restapi.exception.HttpException;
import dev.rickcloudy.restapi.exception.custom.UserNotFoundException;
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

import java.util.*;
import java.util.stream.Collectors;

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
        List<String> imageKey = new ArrayList<>();
        return userRepository.findById(blogPost.getAuthorId())
                .switchIfEmpty(Mono.error(new UserNotFoundException(HttpStatus.NOT_FOUND, "User Not Found")))
                .flatMap(user -> blogPostsRepository.save(blogPost))
                .flatMap(savedBlogPost -> {
                            return saveImage(images, savedBlogPost.getId())
                                    .doOnNext(res -> {
                                        imageKey.add(res.getImageKey());
                                        log.debug("Key {}", imageKey);
                                    })
                                    .collectList()
                                    .map(savedImages -> {
                                        BlogPostsDTO dto = mapper.blogPostsToBlogPostsDTO(savedBlogPost);
                                        dto.setImages(savedImages);
                                        return dto;
                                    });
                        })
                        .doOnError(err -> {
                            // Delete the uploaded images if an error occurs
                            imageKey.forEach(s3Service::deleteRickCloudyBlogImage);
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

    public Flux<BlogPostsDTO> getAllBlogPosts() {
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
                            .thenReturn(blogPostsDTO);
                });
    }

    @Transactional
    public Mono<BlogPosts> updateBlogPost(Long id, BlogPosts blogPost, Flux<FilePart> images    ) {
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

//    private Mono<List<BlogImages>> handleImageChanges(Long blogPostId, Flux<FilePart> images) {
//        Flux<BlogImages> existingImage = imagesRepository.findByBlogPostId(blogPostId);
//
//        Mono<Set<String>> existingImageKey = existingImage.map(BlogImages::getImageKey)
//                .collect(Collectors.toSet());
//        // Filter out images that are already saved (i.e., they have existing image keys)
//        Flux<FilePart> newImages = images.filterWhen(image -> existingImageKey
//                .map(keys -> !keys.contains(image.filename())));
//
//        // Find the removed images (those that are in the DB but not in the updated form)
//        Mono<Set<String>> newImageKeys = images
//                .map(FilePart::filename) // Get the filenames of new images
//                .collect(Collectors.toSet()); // Collect them into a Set asynchronously
//
//        // You need to wait for `existingImageKey` to resolve asynchronously in a non-blocking way
//        return existingImageKey
//                .zipWith(newImageKeys) // Combine both sets (existing and new images)
//                .flatMap(tuple -> {
//                    Set<String> existingKeys = tuple.getT1(); // Extract existing image keys
//                    Set<String> newKeys = tuple.getT2(); // Extract new image keys
//
//                    // Find removed image keys (those that are in the DB but not in the new form)
//                    Set<String> removedImageKeys = existingKeys.stream()
//                            .filter(key -> !newKeys.contains(key))
//                            .collect(Collectors.toSet());
//
//                    // Delete removed images from the database and the storage
//                    return deleteRemovedImages(removedImageKeys)
//                            .then(saveImage(newImages, existingBlogPost.getId())) // Save the new images
//                            .flatMap(savedImages -> {
//                                // Combine the new saved images with the existing (not deleted) images
//                                List<BlogImages> allImages = existingBlogPost.getImages().stream()
//                                        .filter(image -> !removedImageKeys.contains(image.getImageKey())) // Remove the deleted images
//                                        .collect(Collectors.toList());
//                                allImages.addAll(savedImages); // Add the newly saved images
//                                return Mono.just(allImages); // Return the updated image list
//                            });
//                });
//    }

    private Mono<Void>  deleteRemovedImages(Set<String> removedImageKeys) {
        if (removedImageKeys.isEmpty()) {
            return Mono.empty(); // No images to delete
        }
        return imagesRepository.findAllByImageKeys(removedImageKeys)
                .flatMap(blogImages -> s3Service.deleteRickCloudyBlogImage(blogImages.getImageKey())
                        .then(imagesRepository.delete(blogImages)))
                .then();
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
