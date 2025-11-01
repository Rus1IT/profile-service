package com.cashpilot.userservice.service;

import com.cashpilot.userservice.exception.ValidationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ValidatorService {

    @FunctionalInterface
    public interface ValidatorAction {
        void run() throws io.envoyproxy.pgv.ValidationException;
    }

    public void validate(ValidatorAction action) {
        try {
            action.run();
        } catch (io.envoyproxy.pgv.ValidationException e) {
            log.warn("Validation failed: {}", e.getMessage(), e);
            throw new ValidationException(e.getMessage());
        }
    }
}
