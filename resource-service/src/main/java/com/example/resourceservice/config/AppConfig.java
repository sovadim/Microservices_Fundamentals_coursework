package com.example.resourceservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.BucketAlreadyExistsException;
import software.amazon.awssdk.services.s3.model.BucketAlreadyOwnedByYouException;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;

import java.net.URI;

@Configuration
public class AppConfig {

    @Value("${s3.endpoint}")
    private String s3Endpoint;

    @Value("${s3.access-key}")
    private String s3AccessKey;

    @Value("${s3.secret-key}")
    private String s3SecretKey;

    @Value("${s3.bucket}")
    private String s3Bucket;

    @Value("${s3.region}")
    private String s3Region;

    @Bean
    @LoadBalanced
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    public S3Client s3Client() {
        S3Client client = S3Client.builder()
                .endpointOverride(URI.create(s3Endpoint))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(s3AccessKey, s3SecretKey)))
                .region(Region.of(s3Region))
                .forcePathStyle(true)
                .build();

        try {
            client.createBucket(CreateBucketRequest.builder().bucket(s3Bucket).build());
        } catch (BucketAlreadyExistsException | BucketAlreadyOwnedByYouException ignored) {
        }

        return client;
    }
}
