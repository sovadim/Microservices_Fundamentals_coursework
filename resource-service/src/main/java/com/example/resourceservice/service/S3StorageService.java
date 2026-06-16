package com.example.resourceservice.service;

import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Service
public class S3StorageService {

    private final S3Client s3Client;

    public S3StorageService(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    public void upload(String bucket, String key, byte[] data) {
        s3Client.putObject(
                PutObjectRequest.builder().bucket(bucket).key(key).build(),
                RequestBody.fromBytes(data));
    }

    public byte[] download(String bucket, String key) {
        return s3Client.getObjectAsBytes(
                GetObjectRequest.builder().bucket(bucket).key(key).build()).asByteArray();
    }

    public void delete(String bucket, String key) {
        s3Client.deleteObject(
                DeleteObjectRequest.builder().bucket(bucket).key(key).build());
    }

    public void copy(String srcBucket, String srcKey, String dstBucket, String dstKey) {
        s3Client.copyObject(CopyObjectRequest.builder()
                .sourceBucket(srcBucket)
                .sourceKey(srcKey)
                .destinationBucket(dstBucket)
                .destinationKey(dstKey)
                .build());
    }
}
