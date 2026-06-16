package com.example.resourceservice.messaging;

import com.example.resourceservice.service.ResourceService;
import jakarta.jms.JMSException;
import jakarta.jms.TextMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
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
    public void onMessage(TextMessage textMessage) throws JMSException {
        String traceId = textMessage.getStringProperty("traceId");
        if (traceId != null) {
            MDC.put("traceId", traceId);
        }

        try {
            String message = textMessage.getText();
            Integer resourceId = Integer.parseInt(message.trim());
            log.info("Received processed event for resourceId={}", resourceId);
            resourceService.moveToPermanent(resourceId);
            log.info("Moved resourceId={} to PERMANENT storage", resourceId);
        } finally {
            MDC.remove("traceId");
        }
    }
}
