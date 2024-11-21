package dev.rickcloudy.restapi.service;

import dev.rickcloudy.restapi.dto.UploadResult;
import dev.rickcloudy.restapi.exception.custom.FileUploadException;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.ObjectCannedACL;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

import java.nio.ByteBuffer;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class S3Service {
    private static Logger log = LogManager.getLogger(S3Service.class);
    private final S3AsyncClient s3Client;
    @Value("${rickcloudy.blog.blogImagesBucket}")
    private String bucketName;

    private Mono<UploadResult> uploadRickCloudyBlogImage(String key, ByteBuffer fileData) {
        return Mono.fromFuture(() -> s3Client.putObject(
                PutObjectRequest.builder()
                        .bucket(bucketName)
                        .key(key)
                        .acl(ObjectCannedACL.PUBLIC_READ) // Make the object publicly readable
                        .contentDisposition("inline") // Add this line to make the image display in the browser
                        .build(),
                AsyncRequestBody.fromByteBuffer(fileData)
        )).thenReturn(new UploadResult("https://" + bucketName + ".s3.ap-southeast-2.amazonaws.com/" + key, key));
    }
    public Mono<Void> deleteRickCloudyBlogImage(String key) {
        return Mono.fromFuture(() -> {
                    log.debug("Trying to delete image from something");
                    return s3Client.deleteObject(builder -> builder.bucket(bucketName).key(key));
                })
                .then();
    }
    public Flux<UploadResult> uploadBlogImages(Flux<FilePart> filePartFlux) {
        return filePartFlux.flatMap(filePart -> {
            Flux<DataBuffer> content = filePart.content();
            return content
                    .map(dataBuffer -> {
                        byte[] bytes = new byte[dataBuffer.readableByteCount()];
                        dataBuffer.read(bytes);
                        return ByteBuffer.wrap(bytes);
                    })
                    .collectList()
                    .flatMap(list -> {
                        ByteBuffer combined = ByteBuffer.allocate(list.stream().mapToInt(ByteBuffer::remaining).sum());
                        list.forEach(combined::put);
                        String uniqueKey = UUID.randomUUID().toString() + "_" + filePart.filename();
                        return uploadRickCloudyBlogImage(uniqueKey, combined);
                    });
        })
                .doOnNext(res -> log.info("File uploaded successfully: {}", res.getUrl()))
                .doOnComplete(() -> log.info("All files uploaded successfully"))
                .onErrorMap(err -> {
                    log.error("Error uploading file: {}", err.getMessage());
                    return new FileUploadException(HttpStatus.BAD_REQUEST, "Error while uploading blog image");
                });
    }
}
