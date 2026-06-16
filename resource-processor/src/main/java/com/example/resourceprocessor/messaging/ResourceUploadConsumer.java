package com.example.resourceprocessor.messaging;

import com.example.resourceprocessor.client.ResourceServiceClient;
import com.example.resourceprocessor.client.SongServiceClient;
import com.example.resourceprocessor.service.Mp3MetadataExtractor;
import jakarta.jms.JMSException;
import jakarta.jms.TextMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

@Component
public class ResourceUploadConsumer {

    private static final Logger log = LoggerFactory.getLogger(ResourceUploadConsumer.class);

    private final ResourceServiceClient resourceServiceClient;
    private final SongServiceClient songServiceClient;
    private final Mp3MetadataExtractor metadataExtractor;
    private final ResourceProcessedProducer resourceProcessedProducer;

    public ResourceUploadConsumer(ResourceServiceClient resourceServiceClient,
                                  SongServiceClient songServiceClient,
                                  Mp3MetadataExtractor metadataExtractor,
                                  ResourceProcessedProducer resourceProcessedProducer) {
        this.resourceServiceClient = resourceServiceClient;
        this.songServiceClient = songServiceClient;
        this.metadataExtractor = metadataExtractor;
        this.resourceProcessedProducer = resourceProcessedProducer;
    }

    @JmsListener(destination = "${resource.upload.queue}")
    public void onMessage(TextMessage textMessage) throws JMSException {
        String traceId = textMessage.getStringProperty("traceId");
        if (traceId != null) {
            MDC.put("traceId", traceId);
        }

        try {
            String message = textMessage.getText();
            Integer resourceId = Integer.parseInt(message.trim());
            log.info("Received upload event for resource id: {}", resourceId);

            byte[] data = resourceServiceClient.getResource(resourceId);
            var metadata = metadataExtractor.extract(data);
            metadata.setId(resourceId);

            songServiceClient.createSong(metadata);
            log.info("Song metadata saved for resource id: {}", resourceId);

            resourceProcessedProducer.sendResourceId(resourceId);
            log.info("Sent processed event for resource id: {}", resourceId);
        } finally {
            MDC.remove("traceId");
        }
    }
}
