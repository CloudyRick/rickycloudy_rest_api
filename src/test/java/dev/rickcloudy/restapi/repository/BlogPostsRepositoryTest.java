package dev.rickcloudy.restapi.repository;

import dev.rickcloudy.restapi.config.TestContainerBeanConfiguration;
import dev.rickcloudy.restapi.config.UnitTestingContainerized;
import dev.rickcloudy.restapi.entity.BlogPosts;
import dev.rickcloudy.restapi.enums.BlogStatus;
import dev.rickcloudy.restapi.enums.UserStatus;
import dev.rickcloudy.restapi.entity.Users;
import dev.rickcloudy.restapi.exception.HttpException;
import dev.rickcloudy.restapi.helper.ReactiveLogger;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertNotNull;

@SpringBootTest
@ActiveProfiles("test-container")
@Import(TestContainerBeanConfiguration.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
//@UnitTestingContainerized
class BlogPostsRepositoryTest {

    @Autowired
    private BlogPostsRepository blogRepository;
    @Autowired
    private UserRepository userRepository;

    private Logger log = LogManager.getLogger(BlogPostsRepositoryTest.class);
    private Long userId = 102931111100L;
    @BeforeEach
    void setUp() {
        blogRepository.deleteAll().block();
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
        blogRepository.deleteAll().block();
        userRepository.deleteAll().block();
    }

    @Test
    void save_given_NewBlogPost_when_SaveBlogPost_then_ReturnBlogPost() {
        BlogPosts blogPost = BlogPosts.builder()
                .title("Nama Nama Gajah")
                .content("Ini adalah nama nama gajah yang ada di kebun binatang")
                .authorId(userId)
                .status(BlogStatus.DRAFT)
                .createdAt(ZonedDateTime.now())
                .build();
        // Set the properties of the blogPost object

        Mono<BlogPosts> create = blogRepository.save(blogPost)
                .flatMap(r -> blogRepository.findById(r.getId()));

        StepVerifier.create(create)
                .expectNext(blogPost)
                .verifyComplete();
    }
    @Test
    void save_given_NewBlogPost_when_authorIdNotExists_then_ReturnBlogPost() {
        BlogPosts blogPost = BlogPosts.builder()
                .title("Nama Nama Gajah")
                .content("Ini adalah nama nama gajah yang ada di kebun binatang")
                .authorId(22222L)
                .createdAt(ZonedDateTime.now())
                .build();
        // Set the properties of the blogPost object

        Mono<BlogPosts> create = blogRepository.save(blogPost)
                .doOnError(err -> log.error("Error: {}", err))
                .flatMap(r -> blogRepository.findById(r.getId()));

        StepVerifier.create(create)
                .expectError()
                .verify();
    }

    @Test
    void save_given_BlankBlogPost_when_SaveBlogPost_then_ReturnError() {
        // Create a BlogPost with blank fields
        BlogPosts blogPost = BlogPosts.builder()
                .title("")
                .content("")
                .authorId(null)
                .createdAt(ZonedDateTime.now())
                .build();

        // Try to save the blank BlogPost
        Mono<BlogPosts> create = blogRepository.save(blogPost);

        // Verify that an error is returned
        StepVerifier.create(create)
                .expectError()
                .verify();
    }

    @Test
    void update_given_ExistingBlogPost_when_UpdateBlogPost_then_ReturnUpdatedBlogPost() {
        // Create and save a new BlogPost
        BlogPosts blogPost = BlogPosts.builder()
                .title("Nama Nama Gajah")
                .content("Ini adalah nama nama gajah yang ada di kebun binatang")
                .authorId(userId)
                .status(BlogStatus.PUBLISHED)
                .createdAt(ZonedDateTime.now())
                .build();

        Mono<BlogPosts> create = blogRepository.save(blogPost)
                .flatMap(r -> blogRepository.findById(r.getId()));

        // Verify that the BlogPost was saved correctly
        StepVerifier.create(create)
                .expectNext(blogPost)
                .verifyComplete();

        // Update the BlogPost
        blogPost.setTitle("Updated Title");
        Mono<BlogPosts> update = blogRepository.save(blogPost)
                .flatMap(r -> blogRepository.findById(r.getId()));

        // Verify that the BlogPost was updated correctly
        StepVerifier.create(update)
                .expectNextMatches(updatedBlogPost -> updatedBlogPost.getTitle().equals("Updated Title"))
                .verifyComplete();
    }

    @Test
    void update_given_NonExistingBlogPost_when_UpdateBlogPost_then_ReturnError() {
        // Create a BlogPost that does not exist in the database
        BlogPosts blogPost = BlogPosts.builder()
                .id(99999L)
                .title("Non-existing title")
                .content("Non-existing content")
                .authorId(userId)
                .createdAt(ZonedDateTime.now())
                .build();

        // Try to update the non-existing BlogPost
        Mono<BlogPosts> update = blogRepository.save(blogPost);

        // Verify that an error is returned
        StepVerifier.create(update)
                .expectError()
                .verify();
    }

    @Test
    void update_given_ExistingBlogPost_when_PartialUpdateBlogPost_then_ReturnUpdatedBlogPost() {
        // Create and save a new BlogPost
        BlogPosts blogPost = BlogPosts.builder()
                .title("Nama Nama Gajah")
                .content("Ini adalah nama nama gajah yang ada di kebun binatang")
                .authorId(userId)
                .status(BlogStatus.ARCHIVED)
                .createdAt(ZonedDateTime.now())
                .build();

        // Save the blog post and retrieve it to confirm it was saved correctly
        Mono<BlogPosts> create = blogRepository.save(blogPost)
                .flatMap(savedPost -> {
                    Assertions.assertNotNull(savedPost.getId());  // Ensure ID was generated
                    blogPost.setId(savedPost.getId());  // Set the ID in our test instance for later use
                    return Mono.just(savedPost);
                });

        // Verify that the BlogPost was saved correctly
        StepVerifier.create(create)
                .expectNextMatches(savedPost -> savedPost.getContent().equals("Ini adalah nama nama gajah yang ada di kebun binatang"))
                .verifyComplete();

        // Update the content of the saved BlogPost
        blogPost.setContent("Kleeeek");
        Mono<BlogPosts> update = blogRepository.save(blogPost)
                .flatMap(r -> blogRepository.findById(r.getId()));

        // Verify that the BlogPost was updated with the new content
        StepVerifier.create(update)
                .expectNextMatches(updatedBlogPost -> updatedBlogPost.getContent().equals("Kleeeek"))
                .verifyComplete();
    }

    @Test
    void update_given_ExistingBlogPost_when_UpdateBlogPostWithInvalidData_then_ReturnError() {
        // Create and save a new BlogPost
        BlogPosts blogPost = BlogPosts.builder()
                .title("Nama Nama Gajah")
                .content("Ini adalah nama nama gajah yang ada di kebun binatang")
                .authorId(userId)
                .status(BlogStatus.PUBLISHED)
                .createdAt(ZonedDateTime.now())
                .build();

        Mono<BlogPosts> create = blogRepository.save(blogPost)
                .flatMap(r -> blogRepository.findById(r.getId()));

        // Verify that the BlogPost was saved correctly
        StepVerifier.create(create)
                .expectNext(blogPost)
                .verifyComplete();

        // Update the BlogPost with invalid data
        blogPost.setTitle(null);
        Mono<BlogPosts> update = blogRepository.save(blogPost);

        // Verify that an error is returned
        StepVerifier.create(update)
                .expectError()
                .verify();
    }

    @Test
    void update_given_ExistingBlogPost_when_ConcurrentUpdateBlogPost_then_ReturnUpdatedBlogPost() throws InterruptedException {
        // Create and save a new BlogPost
        BlogPosts blogPost = BlogPosts.builder()
                .title("Nama Nama Gajah")
                .content("Ini adalah nama nama gajah yang ada di kebun binatang")
                .authorId(userId)
                .status(BlogStatus.PUBLISHED)
                .createdAt(ZonedDateTime.now())
                .build();

        Mono<BlogPosts> create = blogRepository.save(blogPost)
                .flatMap(r -> blogRepository.findById(r.getId()));

        // Verify that the BlogPost was saved correctly
        StepVerifier.create(create)
                .expectNext(blogPost)
                .verifyComplete();

        // Update the BlogPost concurrently
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch latch = new CountDownLatch(2);

        executor.submit(() -> {
            blogPost.setTitle("Updated Title 1");
            blogRepository.save(blogPost).block();
            latch.countDown();
        });

        executor.submit(() -> {
            blogPost.setTitle("Updated Title 2");
            blogRepository.save(blogPost).block();
            latch.countDown();
        });

        latch.await();

        // Verify that the BlogPost was updated correctly
        Mono<BlogPosts> updatedBlogPost = blogRepository.findById(blogPost.getId());
        StepVerifier.create(updatedBlogPost)
                .expectNextMatches(u -> u.getTitle().equals("Updated Title 1") || u.getTitle().equals("Updated Title 2"))
                .verifyComplete();
    }

    @Test
    void update_given_ExistingBlogPost_when_IdempotentUpdateBlogPost_then_ReturnUpdatedBlogPost() {
        // Create and save a new BlogPost
        BlogPosts blogPost = BlogPosts.builder()
                .title("Nama Nama Gajah")
                .content("Ini adalah nama nama gajah yang ada di kebun binatang")
                .authorId(userId)
                .status(BlogStatus.PUBLISHED)
                .createdAt(ZonedDateTime.now())
                .build();

        Mono<BlogPosts> create = blogRepository.save(blogPost)
                .flatMap(r -> blogRepository.findById(r.getId()));

        // Verify that the BlogPost was saved correctly
        StepVerifier.create(create)
                .expectNext(blogPost)
                .verifyComplete();

        // Update the BlogPost twice with the same data
        blogPost.setTitle("Updated Title");
        Mono<BlogPosts> update = blogRepository.save(blogPost)
                .then(blogRepository.save(blogPost))
                .then(blogRepository.findById(blogPost.getId()));

        // Verify that the BlogPost was updated correctly
        StepVerifier.create(update)
                .expectNextMatches(u -> u.getTitle().equals("Updated Title"))
                .verifyComplete();
    }

    @Test
    void findById_given_ExistingId_then_ReturnBlogPost() {
        BlogPosts blogPost = BlogPosts.builder()
                .title("Nama Nama Gajah")
                .content("Ini adalah nama nama gajah yang ada di kebun binatang")
                .authorId(userId)
                .createdAt(ZonedDateTime.now())
                .build();

        BlogPosts blog = blogRepository.save(blogPost)
                .flatMap(r -> {
                    System.out.println(r.getId());
                    return blogRepository.findById(r.getId());
                }).block();
        log.info("Test asik {}", blog);
        // Assuming there is a BlogPost with ID 1
        StepVerifier.create(blogRepository.findById(blog.getId()))
                .expectNextCount(1)
                .verifyComplete();
    }

    @Test
    void findById_given_NonExistingId_then_CompleteWithoutItems() {
        // Assuming there is no BlogPost with ID 999
        StepVerifier.create(blogRepository.findById(999L).log())
                .verifyComplete();
    }

    @Test
    void findAll_when_MultipleBlogPostsExist_then_ReturnAllBlogPosts() {
        // Create and save multiple blog posts
        BlogPosts blogPost1 = BlogPosts.builder()
                .title("Title 1")
                .content("Content 1")
                .authorId(userId)
                .status(BlogStatus.PUBLISHED)
                .createdAt(ZonedDateTime.now())
                .build();

        BlogPosts blogPost2 = BlogPosts.builder()
                .title("Title 2")
                .content("Content 2")
                .authorId(userId)
                .status(BlogStatus.PUBLISHED)
                .createdAt(ZonedDateTime.now())
                .build();

        blogRepository.save(blogPost1).block();
        blogRepository.save(blogPost2).block();
        Flux<BlogPosts> all = blogRepository.findAll()
                .doOnNext(blogPosts -> log.info("Blog Post: {}", blogPosts))
                .doOnComplete(() -> log.info("Completed"));

        // Call findAll and verify that it returns all blog posts
        StepVerifier.create(all)
                .expectNext(blogPost1, blogPost2)
                .verifyComplete();
    }

    // For the database error condition, it's a bit tricky to simulate a database error in a unit test.
    // You might need to mock your repository to throw an exception when findById is called.
    // This typically involves using a library like Mockito.

    @Test
    void delete_given_ExistingId_then_DeleteSuccessfully() {
        // Create and save a new blog post
        BlogPosts blogPost = BlogPosts.builder()
                .title("Title")
                .content("Content")
                .authorId(userId)
                .createdAt(ZonedDateTime.now())
                .build();

        BlogPosts posts = blogRepository.save(blogPost).block();

        // Delete the blog post
        Mono<Void> delete = blogRepository.deleteById(blogPost.getId());

        // Verify that the delete operation completed successfully
        StepVerifier.create(delete)
                .verifyComplete();

        // Verify that the blog post no longer exists
        StepVerifier.create(blogRepository.findById(blogPost.getId()))
                .verifyComplete();
    }

    @Test
    void delete_given_NonExistingId_then_CompleteWithoutDeleting() {
        // Try to delete a blog post with a non-existing ID
        Mono<Void> delete = ReactiveLogger.logMono(blogRepository.deleteById(999L));

        // Verify that the delete operation completed successfully but didn't delete anything
        StepVerifier.create(delete)
                .verifyComplete();
    }

    @Test
    void delete_given_ConcurrentDeleteRequests_when_DeleteBlogPost_then_HandleConcurrency() throws InterruptedException {
        // Create and save a new blog post
        BlogPosts blogPost = BlogPosts.builder()
                .title("Title")
                .content("Content")
                .authorId(userId)
                .createdAt(ZonedDateTime.now())
                .build();

        BlogPosts posts = blogRepository.save(blogPost).block();

        // Create an ExecutorService and a CountDownLatch
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch latch = new CountDownLatch(2);

        // Submit two concurrent delete requests
        executor.submit(() -> {
            try {
                latch.countDown();
                latch.await();
                blogRepository.deleteById(blogPost.getId()).block();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        executor.submit(() -> {
            try {
                latch.countDown();
                latch.await();
                blogRepository.deleteById(blogPost.getId()).block();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        // Shut down the executor
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);
        var blog = ReactiveLogger.logMono(blogRepository.findById(blogPost.getId()));
        // Verify that the blog post no longer exists
        StepVerifier.create(blog)
                .verifyComplete();
    }

    @Test
    void findByParams_Given_ValidParameters_Expect_ReturnMatchingBlogPosts() {
        // Given
        // Create and save a new blog post
        BlogPosts blogPost = BlogPosts.builder()
                .title("Title")
                .content("Content")
                .authorId(userId)
                .createdAt(ZonedDateTime.now())
                .build();

        // Save the blog post
        blogRepository.save(blogPost).block();
        Map<String, String> params = new HashMap<>();
        params.put("title", "Title");

        // When
        Flux<BlogPosts> blogPosts = ReactiveLogger.logFlux(blogRepository.findByParams(params));

        // Expect
        StepVerifier.create(blogPosts)
            .expectNextMatches(res -> res.getTitle().startsWith("Title"))
            .verifyComplete();
    }

    @Test
    void findByParams_Given_ValidParametersAuthorId_Expect_ReturnMatchingBlogPosts() {
        // Given
        // Create and save a new blog post
        BlogPosts blogPost = BlogPosts.builder()
                .title("Title")
                .content("Content")
                .authorId(userId)
                .createdAt(ZonedDateTime.now())
                .build();

        // Save the blog post
        blogRepository.save(blogPost).block();
        Map<String, String> params = new HashMap<>();
        params.put("authorId", userId.toString());

        // When
        Flux<BlogPosts> blogPosts = ReactiveLogger.logFlux(blogRepository.findByParams(params));

        // Expect
        StepVerifier.create(blogPosts)
            .expectNextMatches(res -> res.getTitle().startsWith("Title"))
            .verifyComplete();
    }

    @Test
    void findByParams_Given_NoMatchingBlogPosts_Expect_ReturnEmpty() {
        // Given
        Map<String, String> params = new HashMap<>();
        params.put("title", "NonExistingTitle");

        // When
        Flux<BlogPosts> blogPosts = ReactiveLogger.logFlux(blogRepository.findByParams(params));

        // Expect
        StepVerifier.create(blogPosts)
            .verifyComplete();
    }

    @Test
    void findByParams_Given_InvalidParameters_Expect_ThrowException() {
        // Given
        Map<String, String> params = new HashMap<>();
        params.put("invalidField", "Value");

        // When
        Flux<BlogPosts> blogPosts = ReactiveLogger.logFlux(blogRepository.findByParams(params));

        // Expect
        StepVerifier.create(blogPosts)
            .expectErrorMatches(throwable -> throwable instanceof HttpException
                    && throwable.getMessage().equals("Invalid parameter name: invalidField"))
            .verify();
    }

}