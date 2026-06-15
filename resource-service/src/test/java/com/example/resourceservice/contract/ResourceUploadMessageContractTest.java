package com.example.resourceservice.contract;

import com.example.resourceservice.client.SongServiceClient;
import com.example.resourceservice.messaging.ResourceUploadProducer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jms.core.JmsTemplate;
import software.amazon.awssdk.services.s3.S3Client;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class ResourceUploadMessageContractTest {

    @MockBean
    private S3Client s3Client;

    @MockBean
    private SongServiceClient songServiceClient;

    @Autowired
    private ResourceUploadProducer producer;

    @Autowired
    private JmsTemplate jmsTemplate;

    @Value("${resource.upload.queue}")
    private String queueName;

    @Test
    void sendResourceId_publishesStringMessageToQueue() {
        producer.sendResourceId(42);

        String received = (String) jmsTemplate.receiveAndConvert(queueName);
        assertThat(received).isEqualTo("42");
    }
}