package dev.rickcloudy.restapi.repository;

import dev.rickcloudy.restapi.config.TestContainerBeanConfiguration;
import dev.rickcloudy.restapi.config.UnitTestingContainerized;
import dev.rickcloudy.restapi.entity.BlogImages;
import dev.rickcloudy.restapi.entity.BlogPosts;
import dev.rickcloudy.restapi.entity.Users;
import dev.rickcloudy.restapi.enums.BlogStatus;
import dev.rickcloudy.restapi.enums.ImageType;
import dev.rickcloudy.restapi.enums.UserStatus;
import dev.rickcloudy.restapi.helper.ReactiveLogger;
import jakarta.validation.ConstraintViolationException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.MySQLContainer;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.ZonedDateTime;
import java.util.NoSuchElementException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test-container")
@Import(TestContainerBeanConfiguration.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BlogImagesRepositoryTest {
    private final Logger log = LogManager.getLogger(BlogImagesRepositoryTest.class);
    @Autowired
    private BlogImagesRepository blogImagesRepository;
    @Autowired
    private BlogPostsRepository blogPostsRepository;
    @Autowired
    private UserRepository usersRepository;
    private Long userId = 112121212L;
    private Long blogPostId = 0L;
    @BeforeEach
    void setUp() {
        blogImagesRepository.deleteAll().block();
        blogPostsRepository.deleteAll().block();
        usersRepository.deleteAll().block();

        Users user = Users.builder()
                .id(userId)
                .firstName("Rickya")
                .lastName("Cahyadi")
                .email("rickycahyadi23@gmail.com1s")
                .username("rckychydii1da")
                .status(UserStatus.ACTIVE)
                .password("anjing123")
                .createdAt(ZonedDateTime.now())
                .build();
        BlogPosts blogPost = BlogPosts.builder()
                .title("Blog Post Title")
                .content("Blog Post Content")
                .authorId(userId)
                .status(BlogStatus.DRAFT)
                .createdAt(ZonedDateTime.now())
                .updatedAt(ZonedDateTime.now())
                .build();
        usersRepository.save(user).block();
        BlogPosts blog = ReactiveLogger.logMono(blogPostsRepository.save(blogPost)).block();
        blogPostId = blog.getId();
        System.out.println("Blog Post ID: " + blogPostId);
    }

    @AfterEach
    void tearDown() {
        blogImagesRepository.deleteAll().block();
        blogPostsRepository.deleteAll().block();
        usersRepository.deleteAll().block();
    }

    @Test
    void save_given_NewBlogImage_when_SaveBlogImage_then_ReturnBlogImage() {

        BlogImages blogImage = createNewBlogImage();
        System.out.println("Blog Image: " + blogImage.toString());

        Mono<BlogImages> create = ReactiveLogger.logMono(blogImagesRepository.save(blogImage))
                .flatMap(r -> blogImagesRepository.findById(r.getId()));

        StepVerifier.create(create)
                .expectNext(blogImage)
                .verifyComplete();
    }

    @Test
    void save_given_NonExistentBlogPostId_when_SaveBlogImage_then_ThrowException() {
        // Given
        BlogImages blogImage = BlogImages.builder()
                .blogPostId(999999L) // This ID doesn't exist in the blog_posts table
                .imageUrl("http://example.com/image.jpg")
                .alt("Example Image")
                .caption("This is an example image")
                .credit("John Doe")
                .type(ImageType.JPEG)
                .createdAt(ZonedDateTime.now())
                .build();

        // When
        Mono<BlogImages> result = ReactiveLogger.logMono(blogImagesRepository.save(blogImage));

        // Then
        StepVerifier.create(result)
                .expectError()
                .verify();
    }

    @Test
    void save_given_NullFields_when_SaveBlogImage_then_ThrowException() {
        // Given
        BlogImages blogImage = BlogImages.builder().build(); // This object has null fields

        // When
        Mono<BlogImages> result = blogImagesRepository.save(blogImage);

        // Then
        StepVerifier.create(result)
                .expectError()
                .verify();
    }

    @Test
    void save_given_ConcurrentSave_when_SaveBlogImage_then_ThrowException() {
        // Given
        BlogImages blogImage1 = createNewBlogImage();
        BlogImages blogImage2 = createNewBlogImage();

        // When
        Mono<BlogImages> result1 = blogImagesRepository.save(blogImage1);
        Mono<BlogImages> result2 = blogImagesRepository.save(blogImage2);

        // Then
        StepVerifier.create(result1)
                .expectNext(blogImage1)
                .verifyComplete();

        StepVerifier.create(result2)
                .expectNext(blogImage2)
                .verifyComplete();
    }

    @Test
    void update_given_ValidBlogImage_when_UpdateBlogImage_then_ReturnUpdatedBlogImage() {
        // Given
        BlogImages blogImage = createNewBlogImage();
        BlogImages saved = blogImagesRepository.save(blogImage).block();
        blogImage.setCaption("Updated Caption");

        // When
        Mono<BlogImages> result = blogImagesRepository.save(blogImage);

        // Then
        StepVerifier.create(result)
                .expectNextMatches(updatedBlogImage -> updatedBlogImage.getCaption().equals("Updated Caption"))
                .verifyComplete();
    }

    @Test
    void update_given_NonExistentBlogPostId_when_UpdateBlogImage_then_ThrowException() {
        // Given
        BlogImages blogImage = createNewBlogImage();
        blogImage.setBlogPostId(999999L); // This ID doesn't exist in the blog_posts table

        // When
        Mono<BlogImages> result = blogImagesRepository.save(blogImage);

        // Then
        StepVerifier.create(result)
                .expectError(DataIntegrityViolationException.class)
                .verify();
    }

    @Test
    void update_given_NullFields_when_UpdateBlogImage_then_ThrowException() {
        // Given
        BlogImages blogImage = BlogImages.builder().build(); // This object has null fields

        // When
        Mono<BlogImages> result = blogImagesRepository.save(blogImage);

        // Then
        StepVerifier.create(result)
                .expectError()
                .verify();
    }

    @Test
    void update_given_ConcurrentUpdate_when_UpdateBlogImage_then_HandleConcurrency() {
        // Given
        BlogImages blogImage1 = createNewBlogImage();
        BlogImages blogImage2 = createNewBlogImage();
        blogImagesRepository.save(blogImage1).block();
        blogImagesRepository.save(blogImage2).block();
        blogImage1.setCaption("Updated Caption 1");
        blogImage2.setCaption("Updated Caption 2");

        // When
        Mono<BlogImages> result1 = blogImagesRepository.save(blogImage1);
        Mono<BlogImages> result2 = blogImagesRepository.save(blogImage2);

        // Then
        StepVerifier.create(result1)
                .expectNextMatches(updatedBlogImage -> updatedBlogImage.getCaption().equals("Updated Caption 1"))
                .verifyComplete();

        StepVerifier.create(result2)
                .expectNextMatches(updatedBlogImage -> updatedBlogImage.getCaption().equals("Updated Caption 2"))
                .verifyComplete();
    }

    @Test
    void update_given_NonExistentBlogImageId_when_UpdateBlogImage_then_ThrowException() {
        // Given
        BlogImages blogImage = createNewBlogImage();
        blogImage.setId(999999L); // This ID doesn't exist in the blog_images table

        // When
        Mono<BlogImages> result = ReactiveLogger.logMono(blogImagesRepository.save(blogImage));

        // Then
        StepVerifier.create(result)
                .expectError()
                .verify();
    }

    @Test
    void deleteByBlogPostId_given_ValidBlogPostId_when_DeleteByBlogPostId_then_DeleteAllImages() {
        // Given
        BlogImages blogImage1 = createNewBlogImage();
        BlogImages blogImage2 = createNewBlogImage();
        blogImagesRepository.save(blogImage1).block();
        blogImagesRepository.save(blogImage2).block();

        // When
        Mono<Void> result = blogImagesRepository.deleteByBlogPostId(blogPostId);

        // Then
        StepVerifier.create(result)
                .verifyComplete();

        // Verify that no images exist for the given blog post ID
        Flux<BlogImages> images = ReactiveLogger.logFlux(blogImagesRepository.findByBlogPostId(blogPostId));
        StepVerifier.create(images)
                .expectNextCount(0)
                .verifyComplete();
    }

    @Test
    void deleteByBlogPostId_given_NonExistentBlogPostId_when_DeleteByBlogPostId_then_CompleteWithoutDeleting() {
        // Given a non-existent blog post ID
        Long nonExistentId = 999999L;

        // When
        Mono<Void> result = ReactiveLogger.logMono(blogImagesRepository.deleteByBlogPostId(nonExistentId));

        // Then
        StepVerifier.create(result)
                .verifyComplete();
    }

    @Test
    void deleteByBlogPostId_given_ConcurrentDeleteRequests_when_DeleteByBlogPostId_then_HandleConcurrency() throws InterruptedException {
        // Given
        BlogImages blogImage1 = createNewBlogImage();
        BlogImages blogImage2 = createNewBlogImage();
        blogImagesRepository.save(blogImage1).block();
        blogImagesRepository.save(blogImage2).block();

        // Create an ExecutorService and a CountDownLatch
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch latch = new CountDownLatch(2);

        // Submit two concurrent delete requests
        executor.submit(() -> {
            try {
                latch.countDown();
                latch.await();
                blogImagesRepository.deleteByBlogPostId(blogPostId).block();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        executor.submit(() -> {
            try {
                latch.countDown();
                latch.await();
                blogImagesRepository.deleteByBlogPostId(blogPostId).block();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        // Shut down the executor
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        // Verify that no images exist for the given blog post ID
        Flux<BlogImages> images = ReactiveLogger.logFlux(blogImagesRepository.findByBlogPostId(blogPostId));
        StepVerifier.create(images)
                .expectNextCount(0)
                .verifyComplete();
    }

    @Test
    void findByBlogPostId_given_ValidBlogPostIdWithImages_when_FindByBlogPostId_then_ReturnImages() {
        // Given a valid blogPostId with associated images
        Long blogPostId = this.blogPostId; // replace with a valid blogPostId
        BlogImages blogImage1 = createNewBlogImage();
        BlogImages blogImage2 = createNewBlogImage();
        blogImagesRepository.save(blogImage1).block();
        blogImagesRepository.save(blogImage2).block();

        // When
        Flux<BlogImages> result = blogImagesRepository.findByBlogPostId(blogPostId);

        // Then
        StepVerifier.create(result)
                .expectNextCount(2) // replace with the expected number of images
                .verifyComplete();
    }

    @Test
    void findByBlogPostId_given_ValidBlogPostIdNoImages_when_FindByBlogPostId_then_ReturnEmpty() {
        // Given a valid blogPostId with no associated images
        Long blogPostId = this.blogPostId; // replace with a valid blogPostId

        // When
        Flux<BlogImages> result = blogImagesRepository.findByBlogPostId(blogPostId);

        // Then
        StepVerifier.create(result)
                .expectNextCount(0)
                .verifyComplete();
    }

    @Test
    void findByBlogPostId_given_InvalidBlogPostId_when_FindByBlogPostId_then_ReturnEmpty() {
        // Given an invalid blogPostId
        Long blogPostId = 999999L; // This ID doesn't exist in the blog_posts table

        // When
        Flux<BlogImages> result = blogImagesRepository.findByBlogPostId(blogPostId);

        // Then
        StepVerifier.create(result)
                .expectNextCount(0)
                .verifyComplete();
    }

    private BlogImages createNewBlogImage() {
        return BlogImages.builder()
                .blogPostId(blogPostId)
                .imageUrl("http://example.com/image.jpg")
                .imageKey("asas/asas_asas_image.jpg")
                .alt("Example Image")
                .caption("This is an example image")
                .credit("John Doe")
                .type(ImageType.JPEG)
                .createdAt(ZonedDateTime.now())
                .updatedAt(ZonedDateTime.now())
                .build();
    }
}