package com.example.resourceservice.dto;

public class StorageDto {

    private Integer id;
    private String storageType;
    private String bucket;
    private String path;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getStorageType() { return storageType; }
    public void setStorageType(String storageType) { this.storageType = storageType; }

    public String getBucket() { return bucket; }
    public void setBucket(String bucket) { this.bucket = bucket; }

    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }
}
