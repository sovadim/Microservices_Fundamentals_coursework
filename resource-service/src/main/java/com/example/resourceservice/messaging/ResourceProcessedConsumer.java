package com.example.resourceservice.messaging;

import com.example.resourceservice.service.ResourceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

@Component
public class ResourceProcessedConsumer {

    private static final Logger log = LoggerFactory.getLogger(ResourceProcessedConsumer.class);

    private final ResourceService resourceService;

    public ResourceProcessedConsumer(ResourceService resourceService) {
        this.resourceService = resourceService;
    }

    @JmsListener(destination = "${resource.processed.queue}")
    public void onMessage(String message) {
        Integer resourceId = Integer.parseInt(message.trim());
        log.info("Received processed event for resourceId={}", resourceId);
        resourceService.moveToPermanent(resourceId);
        log.info("Moved resourceId={} to PERMANENT storage", resourceId);
    }
}
