package com.cashpilot.userservice.service;

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
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.transaction.annotation.Transactional;

@GrpcService
@RequiredArgsConstructor
public class UserProfileServiceImpl extends UserProfileServiceGrpc.UserProfileServiceImplBase {

    private final UserProfileRepository userProfileRepository;
    private final UserProfileMapper userProfileMapper;

    @FunctionalInterface
    private interface ValidatorAction {
        void run() throws io.envoyproxy.pgv.ValidationException;
    }


    private void validate(ValidatorAction action) {
        try {
            action.run();
        } catch (io.envoyproxy.pgv.ValidationException e) {
            throw new ValidationException(e.getMessage());
        }
    }

    @Override
    @Transactional
    public void createUserProfile(CreateUserProfileRequest request, StreamObserver<UserProfileResponse> responseObserver) {
        validate(() -> new CreateUserProfileRequestValidator().assertValid(request, null));

        if (userProfileRepository.existsById(request.getUserId())) {
            throw new AlreadyExistException("User profile already exists with ID: " + request.getUserId());
        }

        UserProfile newUserProfile = userProfileMapper.toEntity(request);
        UserProfile savedProfile = userProfileRepository.save(newUserProfile);

        responseObserver.onNext(userProfileMapper.toResponse(savedProfile));
        responseObserver.onCompleted();
    }

    @Override
    @Transactional(readOnly = true)
    public void getUserProfile(GetUserProfileRequest request, StreamObserver<UserProfileResponse> responseObserver) {
        validate(() -> new GetUserProfileRequestValidator().assertValid(request, null));

        UserProfile profile = userProfileRepository.findById(request.getUserId())
                .orElseThrow(() -> new NotFoundException("User profile not found with ID: " + request.getUserId()));

        responseObserver.onNext(userProfileMapper.toResponse(profile));
        responseObserver.onCompleted();
    }

    @Override
    @Transactional
    public void updateUserProfile(UpdateUserProfileRequest request, StreamObserver<UserProfileResponse> responseObserver) {
        validate(() -> new UpdateUserProfileRequestValidator().assertValid(request, null));

        UserProfile existingProfile = userProfileRepository.findById(request.getUserId())
                .orElseThrow(() -> new NotFoundException("User profile not found to update"));

        userProfileMapper.updateEntityFromRequest(request, existingProfile);
        UserProfile updatedProfile = userProfileRepository.save(existingProfile);

        responseObserver.onNext(userProfileMapper.toResponse(updatedProfile));
        responseObserver.onCompleted();
    }

    @Override
    @Transactional
    public void deleteUserProfile(DeleteUserProfileRequest request, StreamObserver<Empty> responseObserver) {
        UserProfile profile = userProfileRepository.findById(request.getUserId())
                .orElseThrow(() -> new NotFoundException("Cannot delete. User profile not found with ID: " + request.getUserId()));

        userProfileRepository.delete(profile);

        responseObserver.onNext(Empty.getDefaultInstance());
        responseObserver.onCompleted();
    }
}