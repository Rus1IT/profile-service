package com.cashpilot.userservice.service;

import com.cashpilot.userservice.grpc.*;
import com.cashpilot.userservice.entity.UserProfile;
import com.cashpilot.userservice.repository.UserProfileRepository;
import io.envoyproxy.pgv.ValidationException;
import io.envoyproxy.pgv.ValidatorImpl;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.transaction.annotation.Transactional;


import java.util.Optional;

@GrpcService
@RequiredArgsConstructor
public class UserProfileServiceImpl extends UserProfileServiceGrpc.UserProfileServiceImplBase {

    private final UserProfileRepository userProfileRepository;

    @Override
    @Transactional
    public void createUserProfile(CreateUserProfileRequest request, StreamObserver<UserProfileResponse> responseObserver) {
        System.out.println("Request :"+ request);
        UserProfile newUserProfile = new UserProfile();
        newUserProfile.setUserId(request.getUserId());
        newUserProfile.setUsername(request.getUsername());
        newUserProfile.setEmail(request.getEmail());
        newUserProfile.setDefaultCurrency("KZT");

        UserProfile savedProfile = userProfileRepository.save(newUserProfile);

        responseObserver.onNext(mapToResponse(savedProfile));
        responseObserver.onCompleted();
    }

    @Override
    @Transactional(readOnly = true)
    public void getUserProfile(GetUserProfileRequest request, StreamObserver<UserProfileResponse> responseObserver) {
        userProfileRepository.findById(request.getUserId())
                .ifPresentOrElse(
                        profile -> {
                            responseObserver.onNext(mapToResponse(profile));
                            responseObserver.onCompleted();
                        },
                        () -> responseObserver.onError(new RuntimeException("User profile not found"))
                );
    }

    @SneakyThrows
    @Override
    @Transactional
    public void updateUserProfile(UpdateUserProfileRequest request, StreamObserver<UserProfileResponse> responseObserver) {
        new UpdateUserProfileRequestValidator().assertValid(request, null);
        UserProfile existingProfile = userProfileRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("User profile not found to update"));

        if (!request.getFirstName().isEmpty()) {
            existingProfile.setFirstName(request.getFirstName());
        }
        if (!request.getLastName().isEmpty()) {
            existingProfile.setLastName(request.getLastName());
        }
        if (!request.getDefaultCurrency().isEmpty()) {
            existingProfile.setDefaultCurrency(request.getDefaultCurrency());
        }

        UserProfile updatedProfile = userProfileRepository.save(existingProfile);

        responseObserver.onNext(mapToResponse(updatedProfile));
        responseObserver.onCompleted();
    }

    private UserProfileResponse mapToResponse(UserProfile profile) {
        return UserProfileResponse.newBuilder()
                .setUserId(profile.getUserId())
                .setUsername(profile.getUsername())
                .setEmail(profile.getEmail())
                .setFirstName(Optional.ofNullable(profile.getFirstName()).orElse(""))
                .setLastName(Optional.ofNullable(profile.getLastName()).orElse(""))
                .setDefaultCurrency(Optional.ofNullable(profile.getDefaultCurrency()).orElse(""))
                .build();
    }
}