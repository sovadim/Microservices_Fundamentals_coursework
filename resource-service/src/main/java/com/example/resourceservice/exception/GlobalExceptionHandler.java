package com.example.resourceservice.exception;

import com.example.resourceservice.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(e.getMessage(), "404"));
    }

    @ExceptionHandler({InvalidMp3Exception.class, InvalidRequestException.class})
    public ResponseEntity<ErrorResponse> handleBadRequest(RuntimeException e) {
        return ResponseEntity.badRequest()
                .body(new ErrorResponse(e.getMessage(), "400"));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        return ResponseEntity.badRequest()
                .body(new ErrorResponse("Invalid value '" + e.getValue() + "' for ID. Must be a positive integer", "400"));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParam(MissingServletRequestParameterException e) {
        return ResponseEntity.badRequest()
                .body(new ErrorResponse("Required parameter '" + e.getParameterName() + "' is missing", "400"));
    }

    @ExceptionHandler(SongServiceException.class)
    public ResponseEntity<ErrorResponse> handleSongService(SongServiceException e) {
        return ResponseEntity.internalServerError()
                .body(new ErrorResponse(e.getMessage(), "500"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Exception e) {
        return ResponseEntity.internalServerError()
                .body(new ErrorResponse("Internal server error", "500"));
    }
}
