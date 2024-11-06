package dev.rickcloudy.restapi.service;

import dev.rickcloudy.restapi.dto.UploadResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.mock.web.MockPart;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ServerWebInputException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class S3ServiceTest {
    @Autowired
    private S3Service s3Service;
    @Autowired
    private S3AsyncClient s3Client;

    @BeforeEach
    void setUp() {
        s3Client = S3AsyncClient.builder()
                .region(Region.AP_SOUTHEAST_2)
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }

    @Test
    void uploadBlogImages_successfulUpload() throws IOException {
        // Path to the file
        Path path = Paths.get("/home/ricky/Downloads/dice-1502706_640.jpg");

        // Read the file into a DataBuffer
        DataBuffer dataBuffer = new DefaultDataBufferFactory().wrap(Files.readAllBytes(path));

        // Create a FilePart from the DataBuffer
        FilePart filePart = new MockFilePart(path.getFileName().toString(), Flux.just(dataBuffer));
        // Execute the service method
        Flux<UploadResult> resultFlux = s3Service.uploadBlogImages(Flux.just(filePart));

        // Verify the result
        StepVerifier.create(resultFlux)
                .expectNextMatches(uploadResult -> {
                    // Verify the URL format
                    boolean urlMatches = uploadResult.getUrl().startsWith("https://rickcloudy-blog-images.s3.ap-southeast-2.amazonaws.com/");
                    // Cleanup the uploaded file
                    String key = uploadResult.getKey();
                    return urlMatches;
                })
                .verifyComplete();
    }

    // Mock FilePart class for testing
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