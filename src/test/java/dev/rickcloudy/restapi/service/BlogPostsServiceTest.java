package dev.rickcloudy.restapi.service;

import dev.rickcloudy.restapi.config.TestContainerBeanConfiguration;
import dev.rickcloudy.restapi.dto.BlogPostsDTO;
import dev.rickcloudy.restapi.entity.BlogImages;
import dev.rickcloudy.restapi.entity.BlogPosts;
import dev.rickcloudy.restapi.enums.BlogStatus;
import dev.rickcloudy.restapi.enums.ImageType;
import dev.rickcloudy.restapi.enums.UserStatus;
import dev.rickcloudy.restapi.entity.Users;
import dev.rickcloudy.restapi.exception.custom.BlogPostNotFoundException;
import dev.rickcloudy.restapi.exception.HttpException;
import dev.rickcloudy.restapi.exception.custom.FileUploadException;
import dev.rickcloudy.restapi.exception.custom.UserNotFoundException;
import dev.rickcloudy.restapi.helper.ReactiveLogger;
import dev.rickcloudy.restapi.repository.BlogImagesRepository;
import dev.rickcloudy.restapi.repository.BlogPostsRepository;
import dev.rickcloudy.restapi.repository.UserRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.test.context.ActiveProfiles;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZonedDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test-container")
@Import(TestContainerBeanConfiguration.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
//@UnitTestingContainerized
class BlogPostsServiceTest {

    private final Logger log = LogManager.getLogger(BlogPostsServiceTest.class);
    @Autowired
    private BlogPostsService blogPostsService;
    @Autowired
    private BlogPostsRepository blogPostsRepository;
    @Autowired
    UserRepository userRepository;
    @Autowired
    BlogImagesRepository imagesRepository;
    @Autowired
    S3Service s3Service;
    private final Long userId = 121212414L;
    @BeforeEach
    void setUp() {
        blogPostsRepository.deleteAll().block();
        userRepository.deleteAll().block();

        Users user = Users.builder()
                .id(userId)
                .firstName("Rickya")
                .lastName("Cahyadi")
                .email("rickycahyadi23@gmail.coms")
                .username("rckychydii1da")
                .status(UserStatus.ACTIVE)
                .password("Anjing123!@#")
                .createdAt(ZonedDateTime.now())
                .build();
        userRepository.save(user).block();
    }

    @AfterEach
    void tearDown() {
        imagesRepository.deleteAll().block();
        blogPostsRepository.deleteAll().block();
        userRepository.deleteAll().block();
    }

    @Test
    void createBlogPost_Given_ValidBlogPostAndImages_When_CreateBlogPost_Expect_BlogPostAndImagesCreated() throws IOException {
        // Given a valid BlogPosts object
        BlogPosts blogPost = BlogPosts.builder()
                .title("Test Title")
                .content("Test Content")
                .authorId(userId)
                .createdAt(ZonedDateTime.now())
                .build();

        // Path to the file
        Path path = Paths.get("/home/ricky/Downloads/dice-1502706_640.jpg");

        // Read the file into a DataBuffer
        DataBuffer dataBuffer = new DefaultDataBufferFactory().wrap(Files.readAllBytes(path));

        // Create a FilePart from the DataBuffer
        FilePart filePart = new MockFilePart(path.getFileName().toString(), Flux.just(dataBuffer));
        Flux<FilePart> images = Flux.just(filePart);

        // When createBlogPost is called
        Mono<BlogPostsDTO> createdBlogPost = blogPostsService.createBlogPost(blogPost, images);

        // Then a BlogPostsDTO object should be created and returned
        StepVerifier.create(createdBlogPost)
                .assertNext(dto -> {
                    assertNotNull(dto.getId());
                    assertEquals(blogPost.getTitle(), dto.getTitle());
                    assertEquals(blogPost.getContent(), dto.getContent());
                    assertEquals(blogPost.getAuthorId(), dto.getAuthorId());
                    assertNotNull(dto.getImages());
                    assertEquals(1, dto.getImages().size());
                })
                .verifyComplete();
    }

    @Test
    void createBlogPost_Given_ValidBlogPostAndNotValidImage_When_SaveImageFails_Expect_Error() {
        // Given a valid BlogPosts object
        BlogPosts blogPost = BlogPosts.builder()
                .title("Test Title")
                .content("Test Content")
                .authorId(userId)
                .createdAt(ZonedDateTime.now())
                .build();

        // Given a Flux<FilePart> object
        DefaultDataBufferFactory bufferFactory = new DefaultDataBufferFactory();
        DataBuffer buffer1 = bufferFactory.wrap("Test Content 1".getBytes());
        DataBuffer buffer2 = bufferFactory.wrap("Test Content 2".getBytes());

        FilePart filePart1 = new MockFilePart("file1", Flux.just(buffer1));
        FilePart filePart2 = new MockFilePart("file2", Flux.just(buffer2));
        FilePart filePart3 = new MockFilePart("file3", Flux.error(new RuntimeException("Anjaay")));
        Flux<FilePart> images = Flux.just(filePart1, filePart2, filePart3);

        // When createBlogPost is called
        Mono<BlogPostsDTO> createdBlogPost = blogPostsService.createBlogPost(blogPost, images);

        // Then an error should be thrown
        StepVerifier.create(createdBlogPost)
                .expectError(FileUploadException.class)
                .verify();
    }
    @Test
    void createBlogPost_Given_NotValidBlogPostAndValidImage_When_SaveImageFails_Expect_Error() {
        // Given a valid BlogPosts object
        BlogPosts blogPost = BlogPosts.builder()
                .title("Test Title")
                .content("Test Content")
                .authorId(11111L)
                .createdAt(ZonedDateTime.now())
                .build();

        // Given a Flux<FilePart> object
        DefaultDataBufferFactory bufferFactory = new DefaultDataBufferFactory();
        DataBuffer buffer1 = bufferFactory.wrap("Test Content 1".getBytes());
        DataBuffer buffer2 = bufferFactory.wrap("Test Content 2".getBytes());

        FilePart filePart1 = new MockFilePart("file1", Flux.just(buffer1));
        FilePart filePart2 = new MockFilePart("file2", Flux.just(buffer2));
        Flux<FilePart> images = Flux.just(filePart1, filePart2);

        // When createBlogPost is called
        Mono<BlogPostsDTO> createdBlogPost = blogPostsService.createBlogPost(blogPost, images);

        // Then an error should be thrown
        StepVerifier.create(createdBlogPost)
                .expectError(UserNotFoundException.class)
                .verify();
    }
    @Test
    void updateBlogPost_Given_ValidBlogPost_When_UpdateBlogPost_Expect_BlogPostUpdated() {
        // Given a valid BlogPosts object
        BlogPosts saveBlog = BlogPosts.builder()
                .title("Nama Nama Anjing")
                .content("Ini adalah nama nama anjing yang ada di kebun binatang")
                .authorId(userId)
                .createdAt(ZonedDateTime.now())
                .build();
        BlogPosts savedBlog = blogPostsRepository.save(saveBlog).block();
        BlogPosts updateBlog = BlogPosts.builder()
                .id(savedBlog.getId())
                .title("Updated Title")
                .content("Updated Content")
                .authorId(userId)
                .createdAt(ZonedDateTime.now())
                .build();

        // When updateBlogPost is called
        Mono<BlogPosts> updatedBlogPost = ReactiveLogger.logMono(blogPostsService.updateBlogPost(savedBlog.getId(), updateBlog));

        // Then the BlogPosts object should be updated and returned
        StepVerifier.create(updatedBlogPost)
                .assertNext(post -> {
                    assertEquals(savedBlog.getId(), post.getId());
                    assertEquals("Updated Title", post.getTitle());
                    assertEquals("Updated Content", post.getContent());
                })
                .verifyComplete();
    }

    @Test
    void updateBlogPost_Given_MismatchedIds_When_UpdateBlogPost_Expect_Exception() {
        // save a BlogPosts object to the database
        BlogPosts saveBlog = BlogPosts.builder()
                .title("Nama Nama Anjing")
                .content("Ini adalah nama nama anjing yang ada di kebun binatang")
                .authorId(userId)
                .createdAt(ZonedDateTime.now())
                .build();
        BlogPosts savedBlog = blogPostsRepository.save(saveBlog).block();
        // Given a BlogPosts object with a mismatched ID
        BlogPosts blogPost = BlogPosts.builder()
                .id(333333L) // Mismatched ID
                .title("Updated Title")
                .content("Updated Content")
                .authorId(userId)
                .createdAt(ZonedDateTime.now())
                .build();

        // When updateBlogPost is called
        Mono<BlogPosts> updatedBlogPost = ReactiveLogger.logMono(blogPostsService.updateBlogPost(saveBlog.getId(), blogPost));

        // Then an exception should be thrown
        StepVerifier.create(updatedBlogPost)
                .expectErrorMatches(throwable -> throwable instanceof HttpException)
                .verify();
    }

    @Test
    void updateBlogPost_Given_MismatchedAuthorIds_When_UpdateBlogPost_Expect_Exception() {
        // save a BlogPosts object to the database
        BlogPosts saveBlog = BlogPosts.builder()
                .title("Nama Nama Anjing")
                .content("Ini adalah nama nama anjing yang ada di kebun binatang")
                .authorId(userId)
                .createdAt(ZonedDateTime.now())
                .build();
        BlogPosts savedBlog = blogPostsRepository.save(saveBlog).block();
        // Given a BlogPosts object with a mismatched author ID
        BlogPosts blogPost = BlogPosts.builder()
                .id(savedBlog.getId())
                .title("Updated Title")
                .content("Updated Content")
                .authorId(212121212L) // Non-existing author ID
                .createdAt(ZonedDateTime.now())
                .build();

        // When updateBlogPost is called
        Mono<BlogPosts> updatedBlogPost = ReactiveLogger.logMono(blogPostsService.updateBlogPost(savedBlog.getId(), blogPost));

        // Then an exception should be thrown
        StepVerifier.create(updatedBlogPost)
                .expectErrorMatches(throwable -> throwable instanceof HttpException)
                .verify();
    }

    @Test
    void updateBlogPost_Given_NonExistingBlogPost_When_UpdateBlogPost_Expect_Exception() {
        // save a BlogPosts object to the database
        BlogPosts saveBlog = BlogPosts.builder()
                .title("Nama Nama Anjing")
                .content("Ini adalah nama nama anjing yang ada di kebun binatang")
                .authorId(userId)
                .createdAt(ZonedDateTime.now())
                .build();
        BlogPosts savedBlog = blogPostsRepository.save(saveBlog).block();
        // Given a BlogPosts object with a non-existing blog post ID
        BlogPosts blogPost = BlogPosts.builder()
                .id(12121212L) // Non-existing blog post ID
                .title("Updated Title")
                .content("Updated Content")
                .authorId(userId)
                .createdAt(ZonedDateTime.now())
                .build();

        // When updateBlogPost is called
        Mono<BlogPosts> updatedBlogPost = ReactiveLogger.logMono(blogPostsService.updateBlogPost(12121212L, blogPost));

        // Then an exception should be thrown
        StepVerifier.create(updatedBlogPost)
                .expectErrorMatches(throwable -> throwable instanceof BlogPostNotFoundException)
                .verify();
    }

    @Test
    void deleteBlogPost_Given_ExistingBlogPost_When_DeleteBlogPost_Expect_BlogPostDeleted() {
        // Given an existing BlogPosts object
        BlogPosts saveBlog = BlogPosts.builder()
                .title("Nama Nama Anjing")
                .content("Ini adalah nama nama anjing yang ada di kebun binatang")
                .authorId(userId)
                .createdAt(ZonedDateTime.now())
                .build();
        BlogPosts savedBlog = blogPostsRepository.save(saveBlog).block();

        // When deleteBlogPost is called
        Mono<BlogPosts> deletedBlogPost = blogPostsService.deleteBlogPost(savedBlog.getId())
                .then(blogPostsRepository.findById(savedBlog.getId()));

        // Then the BlogPosts object should be deleted
        StepVerifier.create(deletedBlogPost)
                .assertNext(post -> {
                    assertEquals(BlogStatus.DELETED, post.getStatus());
                })
                .verifyComplete();
    }

    @Test
    void deleteBlogPost_Given_NonExistingBlogPost_When_DeleteBlogPost_Expect_Exception() {
        // Given a non-existing BlogPosts object
        Long nonExistingBlogPostId = 999L;

        // When deleteBlogPost is called
        Mono<Void> deletedBlogPost = ReactiveLogger.logMono(blogPostsService.deleteBlogPost(nonExistingBlogPostId));

        // Then a BlogPostNotFoundException should be thrown
        StepVerifier.create(deletedBlogPost)
                .expectErrorMatches(throwable -> throwable instanceof BlogPostNotFoundException)
                .verify();
    }

    @Test
    void findByQueryParam_Given_EmptyParam_Expect_EmptyResult() {
        StepVerifier.create(blogPostsService.findByQueryParam(Collections.emptyMap()))
                .expectNextCount(0)
                .verifyComplete();
    }

    @Test
    void findByQueryParam_Given_ValidParam_Expect_BlogPosts() {
        // Given an existing BlogPosts object
        BlogPosts saveBlog = BlogPosts.builder()
                .title("Nama Nama Anjing")
                .content("Ini adalah nama nama anjing yang ada di kebun binatang")
                .authorId(userId)
                .createdAt(ZonedDateTime.now())
                .build();
        BlogPosts savedBlog = blogPostsRepository.save(saveBlog).block();

        Map<String, String> params = new HashMap<>();
        params.put("authorId", userId.toString());

        StepVerifier.create(blogPostsService.findByQueryParam(params))
                .expectNextMatches(blogPost -> blogPost.getAuthorId().equals(userId))
                .verifyComplete();
    }

    @Test
    void findByQueryParam_Given_InvalidParam_Expect_EmptyResult() {
        Map<String, String> params = new HashMap<>();
        params.put("invalidParam", "invalidValue");

        StepVerifier.create(blogPostsService.findByQueryParam(params))
                .expectError(HttpException.class)
                .verify();
    }

    static class MockFilePart implements FilePart {

        private final String filename;
        private final Flux<DataBuffer> contentFlux;

        MockFilePart(String filename, Flux<DataBuffer> contentFlux) {
            this.filename = filename;
            this.contentFlux = contentFlux;
        }

        @Override
        public String filename() {
            return filename;
        }

        @Override
        public Mono<Void> transferTo(Path dest) {
            return Mono.empty();
        }

        @Override
        public String name() {
            return "file";
        }

        @Override
        public HttpHeaders headers() {
            return HttpHeaders.EMPTY;
        }

        @Override
        public Flux<DataBuffer> content() {
            return contentFlux;
        }
    }
}
