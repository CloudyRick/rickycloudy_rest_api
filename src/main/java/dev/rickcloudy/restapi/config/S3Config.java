package dev.rickcloudy.restapi.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;

@Configuration
public class S3Config {
    @Value("${aws.accessKeyId}")
    private String awsId;

    @Value("${aws.secretKey}")
    private String awsKey;

    @Value("${aws.region}")
    private String region;

    @Bean
    public S3AsyncClient s3client() {
        AwsBasicCredentials awsCredentials = AwsBasicCredentials.create(awsId, awsKey);
        return S3AsyncClient.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(awsCredentials))
                .build();
    }
}
