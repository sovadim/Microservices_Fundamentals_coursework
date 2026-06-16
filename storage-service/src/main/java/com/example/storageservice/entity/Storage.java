package com.example.storageservice.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "storages")
public class Storage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "storage_type", nullable = false, length = 20)
    private String storageType;

    @Column(nullable = false)
    private String bucket;

    @Column(nullable = false)
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
