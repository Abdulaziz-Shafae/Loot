package com.example.loot.Api;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.access.AccessDeniedException;

@RestControllerAdvice
public class SafeErrors {
    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<ApiResponse> status(ResponseStatusException e) { return ResponseEntity.status(e.getStatusCode()).body(new ApiResponse(e.getReason()==null ? "Request could not be completed" : e.getReason())); }
    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ApiResponse> denied() { return ResponseEntity.status(403).body(new ApiResponse("Access denied")); }
    @ExceptionHandler({org.springframework.web.bind.MethodArgumentNotValidException.class, jakarta.validation.ConstraintViolationException.class, org.springframework.http.converter.HttpMessageNotReadableException.class, org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class, org.springframework.web.bind.MissingServletRequestParameterException.class})
    ResponseEntity<ApiResponse> validation() { return ResponseEntity.badRequest().body(new ApiResponse("Check the supplied fields and try again")); }
    @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class)
    ResponseEntity<ApiResponse> size() { return ResponseEntity.status(413).body(new ApiResponse("Image must be smaller than 5 MB")); }
    @ExceptionHandler({org.springframework.dao.DataIntegrityViolationException.class, org.springframework.dao.OptimisticLockingFailureException.class})
    ResponseEntity<ApiResponse> conflict() { return ResponseEntity.status(409).body(new ApiResponse("Data changed or is already in use. Refresh and try again.")); }
    @ExceptionHandler({org.springframework.web.client.RestClientException.class,com.example.loot.Service.EmailDeliveryException.class})
    ResponseEntity<ApiResponse> upstream() { return ResponseEntity.status(503).body(new ApiResponse("This service is temporarily unavailable. Try again later.")); }
    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiResponse> other(Exception e) { org.slf4j.LoggerFactory.getLogger(getClass()).error("Request failed: {}",e.getClass().getSimpleName()); return ResponseEntity.internalServerError().body(new ApiResponse("Something went wrong. Please try again.")); }
}
