package com.cashpilot.userservice.service;

import com.cashpilot.userservice.config.SecurityUtil;
import com.cashpilot.userservice.entity.UserProfile;
import com.cashpilot.userservice.exception.AlreadyExistException;
import com.cashpilot.userservice.exception.NotFoundException;
import com.cashpilot.userservice.exception.ValidationException;
import com.cashpilot.userservice.grpc.*;
import com.cashpilot.userservice.mapper.UserProfileMapper;
import com.cashpilot.userservice.repository.UserProfileRepository;
import com.google.protobuf.Empty;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;


@GrpcService
@RequiredArgsConstructor
@Slf4j
public class UserProfileServiceImpl extends UserProfileServiceGrpc.UserProfileServiceImplBase {

    private final UserProfileRepository userProfileRepository;
    private final UserProfileMapper userProfileMapper;
    private final SecurityUtil securityUtil;
    private final ValidatorService validatorService;

    @Override
    @Transactional
    @PreAuthorize("isAuthenticated()")
    public void createUserProfile(CreateUserProfileRequest request, StreamObserver<UserProfileResponse> responseObserver) {
        String userId = securityUtil.getAuthenticatedUserId();

        log.info("Received request to create user profile for userId: {}", userId);
        validatorService.validate(() -> new CreateUserProfileRequestValidator().assertValid(request, null));

        if (userProfileRepository.existsById(userId)) {
            log.warn("User profile already exists for userId: {}. Throwing AlreadyExistException.", userId);
            throw new AlreadyExistException("User profile already exists with ID: " + userId);
        }

        UserProfile newUserProfile = userProfileMapper.toEntity(request);
        newUserProfile.setUserId(userId);

        UserProfile savedProfile = userProfileRepository.save(newUserProfile);

        log.info("Successfully created user profile for userId: {}", savedProfile.getUserId());
        responseObserver.onNext(userProfileMapper.toResponse(savedProfile));
        responseObserver.onCompleted();
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public void getUserProfile(Empty request, StreamObserver<UserProfileResponse> responseObserver) {
        String userId = securityUtil.getAuthenticatedUserId();
        log.info("Received request to get user profile for userId: {}", userId);

        UserProfile profile = userProfileRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("User profile not found for userId: {}. Throwing NotFoundException.", userId);
                    return new NotFoundException("User profile not found with ID: " + userId);
                });

        log.info("Successfully retrieved user profile for userId: {}", profile.getUserId());
        responseObserver.onNext(userProfileMapper.toResponse(profile));
        responseObserver.onCompleted();
    }

    @Override
    @Transactional
    @PreAuthorize("isAuthenticated()")
    public void updateUserProfile(UpdateUserProfileRequest request, StreamObserver<UserProfileResponse> responseObserver) {
        String userId = securityUtil.getAuthenticatedUserId();

        log.info("Received request to update user profile for userId: {}", userId);
        validatorService.validate(() -> new UpdateUserProfileRequestValidator().assertValid(request, null));

        UserProfile existingProfile = userProfileRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("User profile not found for update, userId: {}. Throwing NotFoundException.", userId);
                    return new NotFoundException("User profile not found to update");
                });

        userProfileMapper.updateEntityFromRequest(request, existingProfile);
        UserProfile updatedProfile = userProfileRepository.save(existingProfile);

        log.info("Successfully updated user profile for userId: {}", updatedProfile.getUserId());
        responseObserver.onNext(userProfileMapper.toResponse(updatedProfile));
        responseObserver.onCompleted();
    }

    @Override
    @Transactional
    @PreAuthorize("isAuthenticated()")
    public void deleteUserProfile(Empty request, StreamObserver<Empty> responseObserver) {
        String userId = securityUtil.getAuthenticatedUserId();
        log.info("Received request to delete user profile for userId: {}", userId);


        UserProfile profile = userProfileRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("Cannot delete. User profile not found for userId: {}. Throwing NotFoundException.", userId);
                    return new NotFoundException("Cannot delete. User profile not found with ID: " + userId);
                });

        userProfileRepository.delete(profile);

        log.info("Successfully deleted user profile for userId: {}", userId);
        responseObserver.onNext(Empty.getDefaultInstance());
        responseObserver.onCompleted();
    }
}