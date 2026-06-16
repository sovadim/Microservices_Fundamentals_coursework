package com.example.apigateway.dto;

public class ErrorResponse {

    private final String errorMessage;
    private final String errorCode;
    private final String path;

    public ErrorResponse(String errorMessage, String errorCode, String path) {
        this.errorMessage = errorMessage;
        this.errorCode = errorCode;
        this.path = path;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getPath() {
        return path;
    }
}
