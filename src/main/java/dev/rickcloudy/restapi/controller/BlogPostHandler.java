package dev.rickcloudy.restapi.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.rickcloudy.restapi.dto.ResponseDTO;
import dev.rickcloudy.restapi.entity.BlogPosts;
import dev.rickcloudy.restapi.exception.custom.InvalidJsonException;
import dev.rickcloudy.restapi.exception.custom.UserNotFoundException;
import dev.rickcloudy.restapi.service.BlogPostsService;
import dev.rickcloudy.restapi.service.S3Service;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.http.codec.multipart.FormFieldPart;
import org.springframework.http.codec.multipart.Part;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;


@Component
@RequiredArgsConstructor
public class BlogPostHandler implements Handler {

    private final BlogPostsService blogPostService;
    private final S3Service s3Service;
    private static final Logger log = LogManager.getLogger(BlogPostHandler.class);


    @Override
    public Mono<ServerResponse> save(ServerRequest request) {
        return request.multipartData()
                .flatMap(multiPartMap -> {
                    // Extract the JSON part (assuming the JSON is sent as a "blogPost" field)
                    Part blogPostPart = multiPartMap.getFirst("blogPost");
                    if (blogPostPart == null) {
                        return ServerResponse.badRequest()
                                .body(Mono.just(ResponseDTO.fail(null, "Missing 'blogPost' part in the request")),
                                        ResponseDTO.class);
                    }

                    // Parse the JSON string to a BlogPosts object
                    Mono<BlogPosts> blogPostMono = DataBufferUtils.join(blogPostPart.content())
                            .map(dataBuffer -> {
                                byte[] bytes = new byte[dataBuffer.readableByteCount()];
                                dataBuffer.read(bytes);
                                DataBufferUtils.release(dataBuffer);
                                String jsonString = new String(bytes, StandardCharsets.UTF_8);
                                return jsonString;
                            })
                            .flatMap((jsonString) -> {
                                try {
                                    System.out.println("Received blogPost JSON: " + jsonString);
                                    return Mono.just(new ObjectMapper().readValue(jsonString, BlogPosts.class));
                                } catch (JsonProcessingException e) {
                                    return Mono.error(new InvalidJsonException(HttpStatus.BAD_REQUEST, "Invalid JSON " +
                                            "Request"));
                                }
                            });

                    // Extract the imageUrl parts (assuming they're sent as form field "imageUrl")
                    Flux<String> imageUrl = Flux.fromIterable(multiPartMap.get("imageUrl"))
                            .filter(part -> part instanceof FormFieldPart)
                            .cast(FormFieldPart.class)
                            .map(FormFieldPart::value);

                    // Combine the extracted BlogPosts and file parts to call the service
                    return blogPostMono.flatMap(blogPost ->
                            blogPostService.createBlogPost(blogPost, imageUrl)
                                    .flatMap(blogPostsDTO ->
                                            ServerResponse.ok()
                                                    .contentType(MediaType.APPLICATION_JSON)
                                                    .body(Mono.just(ResponseDTO.success(blogPostsDTO, "Blog has been " +
                                                            "created successfully")), ResponseDTO.class))
                    );
                });
    }

    @Override
    public Mono<ServerResponse> saveAll(ServerRequest request) {
        return null;
    }

    @Override
    public Mono<ServerResponse> findById(ServerRequest request) {
        return null;
    }

    @Override
    public Mono<ServerResponse> update(ServerRequest request) {
        return null;
    }

    @Override
    public Mono<ServerResponse> delete(ServerRequest request) {
        return null;
    }

    @Override
    public Mono<ServerResponse> findAll(ServerRequest request) {
        return null;
    }

    @Override
    public Mono<ServerResponse> findByParams(ServerRequest request) {
        return null;
    }

    public Mono<ServerResponse> uploadBlogImage(ServerRequest request) {
        // Extract the files from the multipart request
        return request.multipartData()
                .flatMap(multipartData -> {
                    // Extract the list of FilePart from the multipart data
                    Flux<FilePart> fileParts = Flux.fromIterable(multipartData.get("files"))
                            .cast(FilePart.class);

                    return s3Service.uploadBlogImages(fileParts) // Pass the Flux of FilePart to the service
                            .collectList() // Collect the results into a list of UploadResult
                            .flatMap(uploadResults -> {
                                // Return the response with the list of upload results (URLs and keys of the uploaded files)
                                return ServerResponse.ok()
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .body(Mono.just(ResponseDTO.success(uploadResults, "Images Uploaded Successfully")),
                                                ResponseDTO.class);
                            });
                })
                .onErrorResume(error -> {
                    log.error("Failed to upload files: {}", error.getMessage());
                    return ServerResponse.status(HttpStatus.BAD_REQUEST)
                            .body(Mono.just(ResponseDTO.fail(null, "Failed to upload files")), ResponseDTO.class);
                });
    }
}
