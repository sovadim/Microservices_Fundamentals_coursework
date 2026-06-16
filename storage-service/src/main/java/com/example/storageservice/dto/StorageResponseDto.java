package com.example.storageservice.dto;

public class StorageResponseDto {

    private Integer id;
    private String storageType;
    private String bucket;
    private String path;

    public StorageResponseDto() {}

    public StorageResponseDto(Integer id, String storageType, String bucket, String path) {
        this.id = id;
        this.storageType = storageType;
        this.bucket = bucket;
        this.path = path;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getStorageType() { return storageType; }
    public void setStorageType(String storageType) { this.storageType = storageType; }

    public String getBucket() { return bucket; }
    public void setBucket(String bucket) { this.bucket = bucket; }

    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }
}
