package com.example.resourceservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "resources")
public class Resource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "s3_key", nullable = false)
    private String s3Key;

    @Column(name = "storage_type", length = 20)
    private String storageType;

    @Column(name = "storage_id")
    private Integer storageId;

    @Column(name = "storage_bucket")
    private String storageBucket;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getS3Key() { return s3Key; }
    public void setS3Key(String s3Key) { this.s3Key = s3Key; }

    public String getStorageType() { return storageType; }
    public void setStorageType(String storageType) { this.storageType = storageType; }

    public Integer getStorageId() { return storageId; }
    public void setStorageId(Integer storageId) { this.storageId = storageId; }

    public String getStorageBucket() { return storageBucket; }
    public void setStorageBucket(String storageBucket) { this.storageBucket = storageBucket; }
}
