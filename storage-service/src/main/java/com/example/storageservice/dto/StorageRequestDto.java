package com.example.storageservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class StorageRequestDto {

    @NotBlank(message = "storageType is required")
    @Pattern(regexp = "STAGING|PERMANENT", message = "storageType must be STAGING or PERMANENT")
    private String storageType;

    @NotBlank(message = "bucket is required")
    private String bucket;

    @NotBlank(message = "path is required")
    private String path;

    public String getStorageType() { return storageType; }
    public void setStorageType(String storageType) { this.storageType = storageType; }

    public String getBucket() { return bucket; }
    public void setBucket(String bucket) { this.bucket = bucket; }

    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }
}
