package com.cashpilot.userservice.mapper;

import com.cashpilot.userservice.entity.UserProfile;
import com.cashpilot.userservice.enums.AppTheme;
import com.cashpilot.userservice.grpc.CreateUserProfileRequest;
import com.cashpilot.userservice.grpc.UpdateUserProfileRequest;
import com.cashpilot.userservice.grpc.UserProfileResponse;
import org.mapstruct.*;

@Mapper(componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface UserProfileMapper {


    UserProfileResponse toResponse(UserProfile userProfile);

    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    UserProfile toEntity(CreateUserProfileRequest request);



    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromRequest(UpdateUserProfileRequest request, @MappingTarget UserProfile userProfile);


    default String mapTheme(AppTheme theme) {
        return theme != null ? theme.name() : null;
    }

    default AppTheme mapTheme(String theme) {
        return theme != null ? AppTheme.valueOf(theme) : null;
    }
}
