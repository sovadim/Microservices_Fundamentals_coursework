package com.example.resourceservice.messaging;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

@Component
public class ResourceUploadProducer {

    private final JmsTemplate jmsTemplate;
    private final String queueName;

    public ResourceUploadProducer(JmsTemplate jmsTemplate,
                                  @Value("${resource.upload.queue}") String queueName) {
        this.jmsTemplate = jmsTemplate;
        this.queueName = queueName;
    }

    public void sendResourceId(Integer resourceId) {
        jmsTemplate.convertAndSend(queueName, resourceId.toString());
    }
}