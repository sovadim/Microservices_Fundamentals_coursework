package com.example.resourceservice.component;

import com.example.resourceservice.client.SongServiceClient;
import com.example.resourceservice.messaging.ResourceUploadProducer;
import com.example.resourceservice.service.S3StorageService;
import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import software.amazon.awssdk.services.s3.S3Client;

@CucumberContextConfiguration
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
public class CucumberSpringConfig {

    @MockBean
    S3Client s3Client;

    @MockBean
    S3StorageService s3StorageService;

    @MockBean
    ResourceUploadProducer uploadProducer;

    @MockBean
    SongServiceClient songServiceClient;
}