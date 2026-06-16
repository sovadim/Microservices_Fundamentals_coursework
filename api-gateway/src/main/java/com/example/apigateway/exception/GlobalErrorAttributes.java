package com.example.apigateway.exception;

import java.net.ConnectException;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.reactive.error.DefaultErrorAttributes;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.server.ResponseStatusException;

import io.netty.channel.ConnectTimeoutException;

@Component
public class GlobalErrorAttributes extends DefaultErrorAttributes {

    @Override
    public Map<String, Object> getErrorAttributes(ServerRequest request, ErrorAttributeOptions options) {
        Throwable error = getError(request);

        HttpStatus status;
        String message;

        if (error instanceof ResponseStatusException rse) {
            status = HttpStatus.valueOf(rse.getStatusCode().value());
            message = status == HttpStatus.NOT_FOUND
                    ? "No route found for this path"
                    : rse.getReason() != null ? rse.getReason() : status.getReasonPhrase();
        } else if (isConnectionError(error)) {
            status = HttpStatus.SERVICE_UNAVAILABLE;
            message = "Service temporarily unavailable";
        } else if (isTimeoutError(error)) {
            status = HttpStatus.GATEWAY_TIMEOUT;
            message = "Service did not respond in time";
        } else {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
            message = "An unexpected error occurred";
        }

        String path = request.path();

        return Map.of(
                "errorMessage", message,
                "errorCode", String.valueOf(status.value()),
                "path", path,
                "status", status.value()
        );
    }

    private boolean isConnectionError(Throwable error) {
        if (error instanceof ConnectException) {
            return true;
        }
        if (error instanceof WebClientRequestException wce) {
            return wce.getCause() instanceof ConnectException;
        }
        if (error.getCause() instanceof ConnectException) {
            return true;
        }
        return false;
    }

    private boolean isTimeoutError(Throwable error) {
        if (error instanceof TimeoutException) return true;
        if (error instanceof ConnectTimeoutException) return true;
        if (error.getCause() instanceof TimeoutException) return true;
        if (error.getCause() instanceof ConnectTimeoutException) return true;
        return false;
    }
}
