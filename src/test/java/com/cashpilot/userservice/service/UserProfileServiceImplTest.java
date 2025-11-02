package com.cashpilot.userservice.service;

import com.cashpilot.userservice.config.SecurityUtil;
import com.cashpilot.userservice.entity.UserProfile;
import com.cashpilot.userservice.enums.AppTheme;
import com.cashpilot.userservice.exception.AlreadyExistException;
import com.cashpilot.userservice.exception.NotFoundException;
import com.cashpilot.userservice.exception.ValidationException;
import com.cashpilot.userservice.grpc.*;
import com.cashpilot.userservice.mapper.UserProfileMapper;
import com.cashpilot.userservice.repository.UserProfileRepository;
import com.google.protobuf.Empty;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@DisplayName("Тесты для UserProfileServiceImpl")
class UserProfileServiceImplTest {


    @Mock
    private UserProfileRepository userProfileRepository;

    @Mock
    private UserProfileMapper userProfileMapper;

    @Mock
    private SecurityUtil securityUtil;

    @Mock
    private ValidatorService validatorService;

    @Mock
    private StreamObserver<UserProfileResponse> userProfileResponseObserver;

    @Mock
    private StreamObserver<Empty> emptyResponseObserver;

    @InjectMocks
    private UserProfileServiceImpl userProfileService;

    private final String TEST_USER_ID = "auth-user-123";
    private UserProfile testUserProfile;
    private UserProfileResponse testUserProfileResponse;

    @BeforeEach
    void setUp() {
        when(securityUtil.getAuthenticatedUserId()).thenReturn(TEST_USER_ID);

        testUserProfile = new UserProfile();
        testUserProfile.setUserId(TEST_USER_ID);
        testUserProfile.setDefaultCurrency("KZT");
        testUserProfile.setLanguage("ru");
        testUserProfile.setTimezone("Asia/Almaty");
        testUserProfile.setBalanceVisibility(true);
        testUserProfile.setTheme(AppTheme.DARK);

        testUserProfileResponse = UserProfileResponse.newBuilder()
                .setUserId(TEST_USER_ID)
                .setDefaultCurrency("KZT")
                .setLanguage("ru")
                .setTimezone("Asia/Almaty")
                .setBalanceVisibility(true)
                .setTheme("DARK")
                .build();
    }


    @Test
    @DisplayName("createUserProfile: должен успешно создать профиль, если он не существует")
    void createUserProfile_shouldSucceed_whenProfileDoesNotExist() {
        CreateUserProfileRequest request = CreateUserProfileRequest.newBuilder()
                .setDefaultCurrency("KZT")
                .setLanguage("ru")
                .setTimezone("Asia/Almaty")
                .setTheme("DARK")
                .build();

        doNothing().when(validatorService).validate(any());

        when(userProfileRepository.existsById(TEST_USER_ID)).thenReturn(false);

        when(userProfileMapper.toEntity(request)).thenReturn(testUserProfile);
        when(userProfileRepository.save(testUserProfile)).thenReturn(testUserProfile);
        when(userProfileMapper.toResponse(testUserProfile)).thenReturn(testUserProfileResponse);

        ArgumentCaptor<UserProfileResponse> responseCaptor = ArgumentCaptor.forClass(UserProfileResponse.class);

        userProfileService.createUserProfile(request, userProfileResponseObserver);

        verify(validatorService).validate(any());
        verify(userProfileRepository).save(testUserProfile);
        verify(userProfileResponseObserver).onNext(responseCaptor.capture());

        assertThat(responseCaptor.getValue().getUserId()).isEqualTo(TEST_USER_ID);
        assertThat(responseCaptor.getValue().getDefaultCurrency()).isEqualTo("KZT");
        assertThat(responseCaptor.getValue().getTheme()).isEqualTo("DARK");

        verify(userProfileResponseObserver).onCompleted();
    }

    @Test
    @DisplayName("createUserProfile: должен выбросить AlreadyExistException, если профиль уже существует")
    void createUserProfile_shouldThrowAlreadyExistException_whenProfileExists() {
        CreateUserProfileRequest request = CreateUserProfileRequest.getDefaultInstance();
        doNothing().when(validatorService).validate(any());
        when(userProfileRepository.existsById(TEST_USER_ID)).thenReturn(true);

        assertThrows(AlreadyExistException.class, () -> {
            userProfileService.createUserProfile(request, userProfileResponseObserver);
        });
        verify(userProfileRepository, never()).save(any());
        verify(userProfileResponseObserver, never()).onNext(any());
    }

    @Test
    @DisplayName("createUserProfile: должен выбросить ValidationException, если валидация не пройдена")
    void createUserProfile_shouldThrowValidationException_whenValidationFails() {
        CreateUserProfileRequest request = CreateUserProfileRequest.getDefaultInstance();
        doThrow(new ValidationException("Validation failed"))
                .when(validatorService).validate(any());

        assertThrows(ValidationException.class, () -> {
            userProfileService.createUserProfile(request, userProfileResponseObserver);
        });
        verify(userProfileRepository, never()).existsById(any());
        verify(userProfileRepository, never()).save(any());
    }


    @Test
    @DisplayName("getUserProfile: должен успешно вернуть профиль, если он найден")
    void getUserProfile_shouldSucceed_whenProfileFound() {
        when(userProfileRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(testUserProfile));
        when(userProfileMapper.toResponse(testUserProfile)).thenReturn(testUserProfileResponse);

        ArgumentCaptor<UserProfileResponse> responseCaptor = ArgumentCaptor.forClass(UserProfileResponse.class);

        userProfileService.getUserProfile(Empty.getDefaultInstance(), userProfileResponseObserver);

        verify(userProfileRepository).findById(TEST_USER_ID);
        verify(userProfileResponseObserver).onNext(responseCaptor.capture());
        verify(userProfileResponseObserver).onCompleted();

        assertThat(responseCaptor.getValue().getUserId()).isEqualTo(TEST_USER_ID);
        assertThat(responseCaptor.getValue().getDefaultCurrency()).isEqualTo("KZT");
    }

    @Test
    @DisplayName("getUserProfile: должен выбросить NotFoundException, если профиль не найден")
    void getUserProfile_shouldThrowNotFoundException_whenProfileNotFound() {
        when(userProfileRepository.findById(TEST_USER_ID)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> {
            userProfileService.getUserProfile(Empty.getDefaultInstance(), userProfileResponseObserver);
        });
        verify(userProfileResponseObserver, never()).onNext(any());
    }


    @Test
    @DisplayName("updateUserProfile: должен успешно обновить профиль")
    void updateUserProfile_shouldSucceed_whenProfileExists() {
        UpdateUserProfileRequest request = UpdateUserProfileRequest.newBuilder()
                .setDefaultCurrency("EUR")
                .setLanguage("en")
                .build();

        UserProfile existingProfile = new UserProfile();
        existingProfile.setUserId(TEST_USER_ID);
        existingProfile.setDefaultCurrency("KZT");
        existingProfile.setLanguage("ru");

        doNothing().when(validatorService).validate(any());
        when(userProfileRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(existingProfile));
        when(userProfileRepository.save(any(UserProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserProfileResponse updatedResponseDTO = UserProfileResponse.newBuilder()
                .setUserId(TEST_USER_ID)
                .setDefaultCurrency("EUR")
                .setLanguage("en")
                .build();

        when(userProfileMapper.toResponse(any(UserProfile.class))).thenReturn(updatedResponseDTO);

        ArgumentCaptor<UserProfileResponse> responseCaptor = ArgumentCaptor.forClass(UserProfileResponse.class);

        userProfileService.updateUserProfile(request, userProfileResponseObserver);

        verify(validatorService).validate(any());
        verify(userProfileMapper).updateEntityFromRequest(request, existingProfile);
        verify(userProfileRepository).save(existingProfile);
        verify(userProfileResponseObserver).onNext(responseCaptor.capture());
        verify(userProfileResponseObserver).onCompleted();

        assertThat(responseCaptor.getValue().getDefaultCurrency()).isEqualTo("EUR");
        assertThat(responseCaptor.getValue().getLanguage()).isEqualTo("en");
    }

    @Test
    @DisplayName("updateUserProfile: должен выбросить NotFoundException, если профиль для обновления не найден")
    void updateUserProfile_shouldThrowNotFoundException_whenProfileNotFound() {
        UpdateUserProfileRequest request = UpdateUserProfileRequest.getDefaultInstance();
        doNothing().when(validatorService).validate(any());
        when(userProfileRepository.findById(TEST_USER_ID)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> {
            userProfileService.updateUserProfile(request, userProfileResponseObserver);
        });
        verify(userProfileRepository, never()).save(any());
    }


    @Test
    @DisplayName("deleteUserProfile: должен успешно удалить профиль")
    void deleteUserProfile_shouldSucceed_whenProfileExists() {
        when(userProfileRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(testUserProfile));
        ArgumentCaptor<Empty> responseCaptor = ArgumentCaptor.forClass(Empty.class);

        userProfileService.deleteUserProfile(Empty.getDefaultInstance(), emptyResponseObserver);

        verify(userProfileRepository).delete(testUserProfile);
        verify(emptyResponseObserver).onNext(responseCaptor.capture());
        assertThat(responseCaptor.getValue()).isEqualTo(Empty.getDefaultInstance());
        verify(emptyResponseObserver).onCompleted();
    }

    @Test
    @DisplayName("deleteUserProfile: должен выбросить NotFoundException, если профиль для удаления не найден")
    void deleteUserProfile_shouldThrowNotFoundException_whenProfileNotFound() {
        when(userProfileRepository.findById(TEST_USER_ID)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> {
            userProfileService.deleteUserProfile(Empty.getDefaultInstance(), emptyResponseObserver);
        });
        verify(userProfileRepository, never()).delete(any());
    }
}