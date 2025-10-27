package com.cashpilot.userservice.mapper;

import com.cashpilot.userservice.entity.UserProfile;
import com.cashpilot.userservice.grpc.CreateUserProfileRequest;
import com.cashpilot.userservice.grpc.UpdateUserProfileRequest;
import com.cashpilot.userservice.grpc.UserProfileResponse;
import org.mapstruct.*;

@Mapper(componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface UserProfileMapper {

    @Mapping(target = "firstName", source = "firstName", defaultValue = "")
    @Mapping(target = "lastName", source = "lastName", defaultValue = "")
    UserProfileResponse toResponse(UserProfile userProfile);


    @Mapping(target = "firstName", ignore = true)
    @Mapping(target = "lastName", ignore = true)
    @Mapping(target = "defaultCurrency", constant = "KZT")
    UserProfile toEntity(CreateUserProfileRequest request);


    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "username", ignore = true)
    @Mapping(target = "email", ignore = true)
    void updateEntityFromRequest(UpdateUserProfileRequest request, @MappingTarget UserProfile userProfile);
}