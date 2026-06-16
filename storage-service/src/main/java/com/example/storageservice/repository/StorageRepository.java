package com.example.storageservice.repository;

import com.example.storageservice.entity.Storage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StorageRepository extends JpaRepository<Storage, Integer> {
    boolean existsByStorageType(String storageType);
}
