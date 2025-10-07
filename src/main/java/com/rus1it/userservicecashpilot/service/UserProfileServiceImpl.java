package com.rus1it.userservicecashpilot.service;

import com.cashpilot.userservice.grpc.*;
import com.rus1it.userservicecashpilot.entity.UserProfile;
import com.rus1it.userservicecashpilot.repository.UserProfileRepository;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
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
                        profile -> responseObserver.onNext(mapToResponse(profile)),
                        () -> responseObserver.onError(new RuntimeException("User profile not found"))
                );
        responseObserver.onCompleted();
    }

    @Override
    @Transactional
    public void updateUserProfile(UpdateUserProfileRequest request, StreamObserver<UserProfileResponse> responseObserver) {
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