package dev.rickcloudy.restapi.controller;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.rickcloudy.restapi.config.TestContainerBeanConfiguration;
import dev.rickcloudy.restapi.entity.BlogPosts;
import dev.rickcloudy.restapi.entity.Users;
import dev.rickcloudy.restapi.enums.UserStatus;
import dev.rickcloudy.restapi.repository.BlogImagesRepository;
import dev.rickcloudy.restapi.repository.BlogPostsRepository;
import dev.rickcloudy.restapi.repository.UserRepository;
import dev.rickcloudy.restapi.service.S3Service;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZonedDateTime;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestContainerBeanConfiguration.class)
@ActiveProfiles("test-container")
public class BlogPostHandlerTest {
    @LocalServerPort
    private int port;

    @Autowired
    WebTestClient client;

    @Autowired
    BlogPostsRepository blogPostsRepository;

    @Autowired
    UserRepository userRepository;

    @Autowired
    BlogImagesRepository blogImagesRepository;

    @Autowired
    S3Service s3Service;
    private final Long userId = 121212414L;
    @BeforeEach
    void setUp() {
        // Clear the database before each test
        blogImagesRepository.deleteAll().block();
        blogPostsRepository.deleteAll().block();
        userRepository.deleteAll().block();
        System.out.println("Database is cleared");

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
        blogImagesRepository.deleteAll().block();
        blogPostsRepository.deleteAll().block();
        userRepository.deleteAll().block();
        System.out.println("Database is cleared");
    }

    @Test
    void givenValidBlogPostAndImages_whenSave_thenBlogPostIsCreatedWithImages() throws IOException, JsonProcessingException {
        // Prepare a valid blog post using the builder pattern
        BlogPosts blogPost = BlogPosts.builder()
                .title("Nama Nama Anjing")
                .content("Ini adalah nama nama anjing yang ada di kebun binatang")
                .authorId(userId)
                .build();

        // Prepare multipart request
        MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
        bodyBuilder.part("blogPost", new ObjectMapper().writeValueAsString(blogPost))
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);

        // Use a file from the local file system
        Path filePath = Paths.get("/home/ricky/Downloads/268979647.jpg"); // Change this to your local file path
        Resource localFileResource = new FileSystemResource(filePath.toFile());
        bodyBuilder.part("files", localFileResource)
                .header(HttpHeaders.CONTENT_DISPOSITION, "form-data; name=\"files\"; filename=\"" + filePath.getFileName() + "\"")
                .header(HttpHeaders.CONTENT_TYPE, MediaType.IMAGE_JPEG_VALUE);

        // When: save is called
        EntityExchangeResult<byte[]> result = client.post()
                .uri("/blogs")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(bodyBuilder.build()))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.title").isEqualTo(blogPost.getTitle())
//                .jsonPath("$.data.content").isEqualTo(blogPost.getContent())
//                .jsonPath("$.images[0].url").value(url -> url.toString().star tsWith("https://s3.amazonaws.com/bucket/"))
                .returnResult();

        // Log the response body
        System.out.println("Response body: " + new String(result.getResponseBody()));
    }

    @Test
    void givenMissingBlogPostField_whenSave_thenReturnsBadRequest() {
        // Prepare multipart request without `blogPost`
        MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
        Path path = Paths.get("src/test/resources/test-image.jpg");
        bodyBuilder.part("files", new FileSystemResource(path))
                .header(HttpHeaders.CONTENT_DISPOSITION, "form-data; name=\"files\"; filename=\"test-image.jpg\"");

        // Send POST request
        EntityExchangeResult<byte[]> result = client.post()
                .uri("/blogs")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(bodyBuilder.build()))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.message").isEqualTo("Missing 'blogPost' part in the request")
                .returnResult();
        System.out.println("Response body: " + new String(result.getResponseBody()));
    }

    @Test
    void givenInvalidJsonInBlogPost_whenSave_thenReturnsInternalServerError() {
        // Prepare multipart request with invalid JSON
        MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
        bodyBuilder.part("blogPost", "{invalidJson}")
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);

        Path path = Paths.get("src/test/resources/test-image.jpg");
        bodyBuilder.part("files", new FileSystemResource(path))
                .header(HttpHeaders.CONTENT_DISPOSITION, "form-data; name=\"files\"; filename=\"test-image.jpg\"");

        // Send POST request
        EntityExchangeResult<byte[]> result = client.post()
                .uri("/blogs")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(bodyBuilder.build()))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.message").isEqualTo("Invalid JSON Request")
                .returnResult();

        System.out.println("Response body: " + new String(result.getResponseBody()));
    }

    @Test
    void givenNonExistentAuthorId_whenSave_thenReturnsNotFound() throws JsonProcessingException {
        // Create a blog post with a non-existent author ID
        BlogPosts blogPost = BlogPosts.builder()
                .title("Test Blog")
                .content("This is a test blog post.")
                .authorId(999L)
                .build();

        MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
        bodyBuilder.part("blogPost", new ObjectMapper().writeValueAsString(blogPost))
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);

        Path path = Paths.get("src/test/resources/test-image.jpg");
        bodyBuilder.part("files", new FileSystemResource(path))
                .header(HttpHeaders.CONTENT_DISPOSITION, "form-data; name=\"files\"; filename=\"test-image.jpg\"");

        client.post()
                .uri("/blogs")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(bodyBuilder.build()))
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.message").isEqualTo("User with ID 999 does not exists");
    }

}
