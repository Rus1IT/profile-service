package com.cashpilot.userservice.config;

import com.cashpilot.userservice.exception.AlreadyExistException;
import com.cashpilot.userservice.exception.NotFoundException;
import com.cashpilot.userservice.exception.ValidationException;
import io.grpc.Status;
import net.devh.boot.grpc.server.advice.GrpcAdvice;
import net.devh.boot.grpc.server.advice.GrpcExceptionHandler;
import org.springframework.security.access.AccessDeniedException;
import lombok.extern.slf4j.Slf4j;

@GrpcAdvice
@Slf4j
public class GrpcExceptionAdvice {

    @GrpcExceptionHandler(AccessDeniedException.class)
    public Status handleAccessDeniedException(AccessDeniedException e) {
        log.warn("Access Denied: {}", e.getMessage());
        return Status.PERMISSION_DENIED
                .withDescription("Доступ запрещен: у вас нет прав на доступ к этому ресурсу.");
    }


    @GrpcExceptionHandler(ValidationException.class)
    public Status handleValidationException(ValidationException e) {
        log.warn("Validation failed: {}", e.getMessage());
        return Status.INVALID_ARGUMENT.withDescription(e.getMessage());
    }


    @GrpcExceptionHandler(NotFoundException.class)
    public Status handleNotFoundException(NotFoundException e) {
        log.info("Resource not found: {}", e.getMessage());
        return Status.NOT_FOUND.withDescription(e.getMessage());
    }


    @GrpcExceptionHandler(AlreadyExistException.class)
    public Status handleAlreadyExistException(AlreadyExistException e) {
        log.info("Resource already exists: {}", e.getMessage());
        return Status.ALREADY_EXISTS.withDescription(e.getMessage());
    }


    @GrpcExceptionHandler(Exception.class)
    public Status handleException(Exception e) {
        log.error("An unexpected error occurred: {}", e.getMessage(), e);
        return Status.INTERNAL.withDescription("Внутренняя ошибка сервера.");
    }
}