package com.example.resourceservice.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.JmsException;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

@Component
public class ResourceUploadProducer {

    private static final Logger log = LoggerFactory.getLogger(ResourceUploadProducer.class);

    private final JmsTemplate jmsTemplate;
    private final String queueName;

    public ResourceUploadProducer(JmsTemplate jmsTemplate,
                                  @Value("${resource.upload.queue}") String queueName) {
        this.jmsTemplate = jmsTemplate;
        this.queueName = queueName;
    }

    @Retryable(
            retryFor = JmsException.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2.0))
    public void sendResourceId(Integer resourceId) {
        jmsTemplate.convertAndSend(queueName, resourceId.toString());
    }

    @Recover
    public void recoverSendResourceId(JmsException e, Integer resourceId) {
        log.error("Failed to publish resourceId={} to queue after all retries", resourceId, e);
        throw new RuntimeException("Message broker unavailable for resourceId=" + resourceId, e);
    }
}