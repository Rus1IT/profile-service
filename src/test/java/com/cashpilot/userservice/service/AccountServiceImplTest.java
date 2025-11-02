package com.cashpilot.userservice.service;

import com.cashpilot.account.proto.*;
import com.cashpilot.userservice.config.SecurityUtil;
import com.cashpilot.userservice.entity.Account;
import com.cashpilot.userservice.entity.UserProfile;
import com.cashpilot.userservice.enums.BankName;
import com.cashpilot.userservice.enums.Currency;
import com.cashpilot.userservice.exception.AlreadyExistException;
import com.cashpilot.userservice.exception.NotFoundException;
import com.cashpilot.userservice.exception.ValidationException;
import com.cashpilot.userservice.mapper.AccountMapper;
import com.cashpilot.userservice.repository.AccountRepository;
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

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Тесты для AccountServiceImpl")
class AccountServiceImplTest {

    @Mock
    private AccountRepository accountRepository;
    @Mock
    private UserProfileRepository userProfileRepository;
    @Mock
    private AccountMapper accountMapper;
    @Mock
    private SecurityUtil securityUtil;
    @Mock
    private ValidatorService validatorService;

    @Mock
    private StreamObserver<AccountProto> accountProtoObserver;
    @Mock
    private StreamObserver<ListAccountsResponse> listAccountsResponseObserver;
    @Mock
    private StreamObserver<Empty> emptyObserver;

    @InjectMocks
    private AccountServiceImpl accountService;

    private final String TEST_USER_ID = "auth-user-123";
    private final UUID TEST_ACCOUNT_ID = UUID.randomUUID();
    private final String TEST_ACCOUNT_ID_STRING = TEST_ACCOUNT_ID.toString();
    private UserProfile testUserProfile;
    private Account testAccountEntity;
    private AccountProto testAccountProto;

    @BeforeEach
    void setUp() {
        // 1. Настраиваем тестовый UserProfile
        testUserProfile = new UserProfile();
        testUserProfile.setUserId(TEST_USER_ID);

        // 2. Настраиваем тестовую Entity Account
        testAccountEntity = new Account();
        testAccountEntity.setAccountId(TEST_ACCOUNT_ID);
        testAccountEntity.setUserProfile(testUserProfile);
        testAccountEntity.setBankName(BankName.KASPI);
        testAccountEntity.setAccountName("My Kaspi");
        testAccountEntity.setCurrency(Currency.KZT);

        // 3. Настраиваем тестовый Proto
        testAccountProto = AccountProto.newBuilder()
                .setAccountId(TEST_ACCOUNT_ID_STRING)
                .setUserProfileId(TEST_USER_ID)
                .setBankName(BankNameProto.KASPI)
                .setAccountName("My Kaspi")
                .setCurrency("KZT")
                .build();
    }


    @Test
    @DisplayName("createAccount: должен успешно создать счет")
    void createAccount_shouldSucceed() {
        // Given
        CreateAccountRequest request = CreateAccountRequest.newBuilder()
                .setBankName(BankNameProto.KASPI)
                .setAccountName("My Kaspi")
                .setCurrency("KZT")
                .build();

        // Моки, необходимые для этого теста
        when(securityUtil.getAuthenticatedUserId()).thenReturn(TEST_USER_ID);
        when(userProfileRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(testUserProfile));

        doNothing().when(validatorService).validate(any());
        when(accountRepository.existsByUserProfileAndBankName(testUserProfile, BankName.KASPI)).thenReturn(false);
        when(accountMapper.createRequestToEntity(request, testUserProfile)).thenReturn(testAccountEntity);
        when(accountRepository.save(testAccountEntity)).thenReturn(testAccountEntity);
        when(accountMapper.entityToProto(testAccountEntity)).thenReturn(testAccountProto);

        // When
        accountService.createAccount(request, accountProtoObserver);

        // Then
        verify(validatorService).validate(any());
        verify(accountRepository).save(testAccountEntity);
        verify(accountProtoObserver).onNext(testAccountProto);
        verify(accountProtoObserver).onCompleted();
    }

    @Test
    @DisplayName("createAccount: должен выбросить AlreadyExistException, если счет в банке уже существует")
    void createAccount_shouldThrowAlreadyExistException_whenAccountExists() {
        // Given
        CreateAccountRequest request = CreateAccountRequest.newBuilder().setBankName(BankNameProto.KASPI).build();

        // Моки, необходимые для этого теста
        when(securityUtil.getAuthenticatedUserId()).thenReturn(TEST_USER_ID);
        when(userProfileRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(testUserProfile));

        doNothing().when(validatorService).validate(any());
        when(accountRepository.existsByUserProfileAndBankName(testUserProfile, BankName.KASPI)).thenReturn(true);

        // When & Then
        assertThrows(AlreadyExistException.class, () -> {
            accountService.createAccount(request, accountProtoObserver);
        });

        verify(accountRepository, never()).save(any());
        verify(accountProtoObserver, never()).onNext(any());
    }

    @Test
    @DisplayName("createAccount: должен выбросить ValidationException, если валидация не пройдена")
    void createAccount_shouldThrowValidationException_whenValidationFails() {
        // Given
        CreateAccountRequest request = CreateAccountRequest.getDefaultInstance();
        doThrow(new ValidationException("Validation failed")).when(validatorService).validate(any());

        // When & Then
        assertThrows(ValidationException.class, () -> {
            accountService.createAccount(request, accountProtoObserver);
        });

        verify(userProfileRepository, never()).findById(any());
    }

    @Test
    @DisplayName("createAccount: должен выбросить NotFoundException, если UserProfile не найден")
    void createAccount_shouldThrowNotFoundException_whenUserNotFound() {
        // Given
        CreateAccountRequest request = CreateAccountRequest.getDefaultInstance();

        // Моки, необходимые для этого теста
        when(securityUtil.getAuthenticatedUserId()).thenReturn(TEST_USER_ID);
        when(userProfileRepository.findById(TEST_USER_ID)).thenReturn(Optional.empty()); // Ключевой мок

        doNothing().when(validatorService).validate(any());

        // When & Then
        assertThrows(NotFoundException.class, () -> {
            accountService.createAccount(request, accountProtoObserver);
        });

        verify(accountRepository, never()).existsByUserProfileAndBankName(any(), any());
    }


    @Test
    @DisplayName("getAccount: должен успешно вернуть счет, если он найден и принадлежит юзеру")
    void getAccount_shouldSucceed_whenFoundAndOwned() {
        // Given
        GetAccountRequest request = GetAccountRequest.newBuilder().setAccountId(TEST_ACCOUNT_ID_STRING).build();

        // Мок для проверки владения
        when(securityUtil.getAuthenticatedUserId()).thenReturn(TEST_USER_ID);

        when(accountRepository.findById(TEST_ACCOUNT_ID)).thenReturn(Optional.of(testAccountEntity));
        when(accountMapper.entityToProto(testAccountEntity)).thenReturn(testAccountProto);

        // When
        accountService.getAccount(request, accountProtoObserver);

        // Then
        verify(accountRepository).findById(TEST_ACCOUNT_ID);
        verify(accountProtoObserver).onNext(testAccountProto);
        verify(accountProtoObserver).onCompleted();
    }

    @Test
    @DisplayName("getAccount: должен выбросить NotFoundException, если счет не найден")
    void getAccount_shouldThrowNotFoundException_whenAccountNotFound() {
        // Given
        GetAccountRequest request = GetAccountRequest.newBuilder().setAccountId(TEST_ACCOUNT_ID_STRING).build();
        when(accountRepository.findById(TEST_ACCOUNT_ID)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(NotFoundException.class, () -> {
            accountService.getAccount(request, accountProtoObserver);
        });

        verify(accountMapper, never()).entityToProto(any());
    }

    @Test
    @DisplayName("getAccount: должен выбросить NotFoundException (Access Denied), если счет принадлежит другому юзеру")
    void getAccount_shouldThrowNotFoundException_whenAccountNotOwned() {
        // Given
        GetAccountRequest request = GetAccountRequest.newBuilder().setAccountId(TEST_ACCOUNT_ID_STRING).build();

        UserProfile otherUser = new UserProfile();
        otherUser.setUserId("other-user-id");
        Account otherAccount = new Account();
        otherAccount.setAccountId(TEST_ACCOUNT_ID);
        otherAccount.setUserProfile(otherUser);

        // Мок для проверки владения (вернет TEST_USER_ID, который не "other-user-id")
        when(securityUtil.getAuthenticatedUserId()).thenReturn(TEST_USER_ID);

        when(accountRepository.findById(TEST_ACCOUNT_ID)).thenReturn(Optional.of(otherAccount));

        // When & Then
        assertThrows(NotFoundException.class, () -> {
            accountService.getAccount(request, accountProtoObserver);
        });
    }

    @Test
    @DisplayName("getAccount: должен выбросить ValidationException, если UUID невалидный")
    void getAccount_shouldThrowValidationException_whenUuidIsInvalid() {
        // Given
        GetAccountRequest request = GetAccountRequest.newBuilder().setAccountId("not-a-valid-uuid").build();

        // When & Then
        assertThrows(ValidationException.class, () -> {
            accountService.getAccount(request, accountProtoObserver);
        });

        verify(accountRepository, never()).findById(any());
    }


    @Test
    @DisplayName("listAccounts: должен вернуть список счетов пользователя")
    void listAccounts_shouldReturnListOfAccounts() {
        // Given
        ListAccountsRequest request = ListAccountsRequest.getDefaultInstance();
        List<Account> userAccounts = List.of(testAccountEntity);
        List<AccountProto> protoAccounts = List.of(testAccountProto);

        // Мок для получения ID пользователя
        when(securityUtil.getAuthenticatedUserId()).thenReturn(TEST_USER_ID);

        when(accountRepository.findAllByUserProfile_UserId(TEST_USER_ID)).thenReturn(userAccounts);
        when(accountMapper.entitiesToProtos(userAccounts)).thenReturn(protoAccounts);

        ArgumentCaptor<ListAccountsResponse> responseCaptor = ArgumentCaptor.forClass(ListAccountsResponse.class);

        // When
        accountService.listAccounts(request, listAccountsResponseObserver);

        // Then
        verify(listAccountsResponseObserver).onNext(responseCaptor.capture());
        verify(listAccountsResponseObserver).onCompleted();

        ListAccountsResponse response = responseCaptor.getValue();
        assertThat(response.getAccountsList()).hasSize(1);
        assertThat(response.getAccountsList().get(0)).isEqualTo(testAccountProto);
    }

    @Test
    @DisplayName("listAccounts: должен вернуть пустой список, если счетов нет")
    void listAccounts_shouldReturnEmptyList_whenNoAccounts() {
        // Given
        ListAccountsRequest request = ListAccountsRequest.getDefaultInstance();

        // Мок для получения ID пользователя
        when(securityUtil.getAuthenticatedUserId()).thenReturn(TEST_USER_ID);

        when(accountRepository.findAllByUserProfile_UserId(TEST_USER_ID)).thenReturn(List.of());
        when(accountMapper.entitiesToProtos(List.of())).thenReturn(List.of());

        ArgumentCaptor<ListAccountsResponse> responseCaptor = ArgumentCaptor.forClass(ListAccountsResponse.class);

        // When
        accountService.listAccounts(request, listAccountsResponseObserver);

        // Then
        verify(listAccountsResponseObserver).onNext(responseCaptor.capture());
        verify(listAccountsResponseObserver).onCompleted();

        assertThat(responseCaptor.getValue().getAccountsList()).isEmpty();
    }


    @Test
    @DisplayName("updateAccount: должен успешно обновить имя счета")
    void updateAccount_shouldSucceed() {
        // Given
        String newName = "My New Kaspi Name";
        UpdateAccountRequest request = UpdateAccountRequest.newBuilder()
                .setAccountId(TEST_ACCOUNT_ID_STRING)
                .setAccountName(newName)
                .build();

        // Мок для проверки владения
        when(securityUtil.getAuthenticatedUserId()).thenReturn(TEST_USER_ID);

        doNothing().when(validatorService).validate(any());
        when(accountRepository.findById(TEST_ACCOUNT_ID)).thenReturn(Optional.of(testAccountEntity));

        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

        AccountProto updatedProto = testAccountProto.toBuilder().setAccountName(newName).build();
        when(accountMapper.entityToProto(any(Account.class))).thenReturn(updatedProto);

        ArgumentCaptor<Account> entityCaptor = ArgumentCaptor.forClass(Account.class);

        // When
        accountService.updateAccount(request, accountProtoObserver);

        // Then
        verify(validatorService).validate(any());
        verify(accountRepository).save(entityCaptor.capture());
        assertThat(entityCaptor.getValue().getAccountName()).isEqualTo(newName);

        verify(accountProtoObserver).onNext(updatedProto);
        verify(accountProtoObserver).onCompleted();
    }

    @Test
    @DisplayName("updateAccount: должен выбросить NotFoundException, если счет не найден")
    void updateAccount_shouldThrowNotFoundException_whenAccountNotFound() {
        // Given
        UpdateAccountRequest request = UpdateAccountRequest.newBuilder()
                .setAccountId(TEST_ACCOUNT_ID_STRING)
                .setAccountName("New Name")
                .build();

        doNothing().when(validatorService).validate(any());
        when(accountRepository.findById(TEST_ACCOUNT_ID)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(NotFoundException.class, () -> {
            accountService.updateAccount(request, accountProtoObserver);
        });

        verify(accountRepository, never()).save(any());
    }


    @Test
    @DisplayName("deleteAccount: должен успешно удалить счет")
    void deleteAccount_shouldSucceed() {
        // Given
        DeleteAccountRequest request = DeleteAccountRequest.newBuilder().setAccountId(TEST_ACCOUNT_ID_STRING).build();

        // Мок для проверки владения
        when(securityUtil.getAuthenticatedUserId()).thenReturn(TEST_USER_ID);

        when(accountRepository.findById(TEST_ACCOUNT_ID)).thenReturn(Optional.of(testAccountEntity));
        doNothing().when(accountRepository).deleteById(TEST_ACCOUNT_ID);

        // When
        accountService.deleteAccount(request, emptyObserver);

        // Then
        verify(accountRepository).findById(TEST_ACCOUNT_ID);
        verify(accountRepository).deleteById(TEST_ACCOUNT_ID);
        verify(emptyObserver).onNext(Empty.getDefaultInstance());
        verify(emptyObserver).onCompleted();
    }

    @Test
    @DisplayName("deleteAccount: должен выбросить NotFoundException, если счет не найден")
    void deleteAccount_shouldThrowNotFoundException_whenAccountNotFound() {
        // Given
        DeleteAccountRequest request = DeleteAccountRequest.newBuilder().setAccountId(TEST_ACCOUNT_ID_STRING).build();

        // Счет не найден
        when(accountRepository.findById(TEST_ACCOUNT_ID)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(NotFoundException.class, () -> {
            accountService.deleteAccount(request, emptyObserver);
        });

        verify(accountRepository, never()).deleteById(any());
    }
}