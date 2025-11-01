package com.cashpilot.userservice.service;

import com.cashpilot.account.proto.*;
import com.cashpilot.account.proto.AccountServiceGrpc.AccountServiceImplBase;
import com.cashpilot.userservice.entity.Account;
import com.cashpilot.userservice.entity.UserProfile;
import com.cashpilot.userservice.enums.BankName;
import com.cashpilot.userservice.exception.AlreadyExistException;
import com.cashpilot.userservice.exception.NotFoundException;
import com.cashpilot.userservice.exception.ValidationException;
import com.cashpilot.userservice.mapper.AccountMapper;
import com.cashpilot.userservice.repository.AccountRepository;
import com.cashpilot.userservice.repository.UserProfileRepository;
import com.cashpilot.userservice.config.SecurityUtil;
import com.google.protobuf.Empty;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class AccountServiceImpl extends AccountServiceImplBase {

    private final AccountRepository accountRepository;
    private final UserProfileRepository userProfileRepository;
    private final AccountMapper accountMapper;
    private final SecurityUtil securityUtil;
    private final ValidatorService validatorService;

    @Override
    @Transactional
    public void createAccount(CreateAccountRequest request, StreamObserver<com.cashpilot.account.proto.AccountProto> responseObserver) {
        log.info("gRPC CreateAccount: bank={}, name={}", request.getBankName(), request.getAccountName());

        validatorService.validate(() -> new CreateAccountRequestValidator().assertValid(request, null));

        UserProfile userProfile = findAuthenticatedUser();
        BankNameProto requestedBankName = request.getBankName();

        if (accountRepository.existsByUserProfileAndBankName(userProfile, convertProtoToEntityEnum(requestedBankName))) {
            log.warn("User {} already has an account with bank {}", userProfile.getUserId(), requestedBankName);
            throw new AlreadyExistException("Account from this bank already exists.");
        }

        Account newAccountEntity = accountMapper.createRequestToEntity(request, userProfile);
        Account savedAccount = accountRepository.save(newAccountEntity);

        log.info("Successfully created account with ID: {}", savedAccount.getAccountId());
        responseObserver.onNext(accountMapper.entityToProto(savedAccount));
        responseObserver.onCompleted();
    }

    @Override
    @Transactional(readOnly = true)
    public void getAccount(GetAccountRequest request, StreamObserver<com.cashpilot.account.proto.AccountProto> responseObserver) {
        log.info("gRPC GetAccount request for ID: {}", request.getAccountId());

        String userId = securityUtil.getAuthenticatedUserId();
        UUID accountId = parseUuid(request.getAccountId());
        Account accountEntity = findAccountByIdAndCheckOwnership(accountId, userId);

        log.info("Successfully retrieved account ID: {}", accountId);
        responseObserver.onNext(accountMapper.entityToProto(accountEntity));
        responseObserver.onCompleted();
    }

    @Override
    @Transactional(readOnly = true)
    public void listAccounts(ListAccountsRequest request, StreamObserver<ListAccountsResponse> responseObserver) {
        log.info("gRPC ListAccounts request received");

        String userId = securityUtil.getAuthenticatedUserId();
        List<Account> userAccounts = accountRepository.findAllByUserProfile_UserId(userId);

        List<AccountProto> protoAccounts = accountMapper.entitiesToProtos(userAccounts);

        ListAccountsResponse response = ListAccountsResponse.newBuilder()
                .addAllAccounts(protoAccounts)
                .build();

        log.info("Successfully retrieved {} accounts for user", protoAccounts.size());
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    @Transactional
    public void updateAccount(UpdateAccountRequest request, StreamObserver<com.cashpilot.account.proto.AccountProto> responseObserver) {
        log.info("gRPC UpdateAccount request for ID: {}", request.getAccountId());

        validatorService.validate(() -> new UpdateAccountRequestValidator().assertValid(request, null));
        
        String userId = securityUtil.getAuthenticatedUserId();
        UUID accountId = parseUuid(request.getAccountId());
        Account accountEntity = findAccountByIdAndCheckOwnership(accountId, userId);

        accountEntity.setAccountName(request.getAccountName());
        Account updatedAccount = accountRepository.save(accountEntity);

        log.info("Successfully updated account ID: {}", updatedAccount.getAccountId());
        responseObserver.onNext(accountMapper.entityToProto(updatedAccount));
        responseObserver.onCompleted();
    }

    @Override
    @Transactional
    public void deleteAccount(DeleteAccountRequest request, StreamObserver<Empty> responseObserver) {
        log.info("gRPC DeleteAccount request for ID: {}", request.getAccountId());

        String userId = securityUtil.getAuthenticatedUserId();
        UUID accountId = parseUuid(request.getAccountId());

        findAccountByIdAndCheckOwnership(accountId, userId);
        accountRepository.deleteById(accountId);

        log.info("Successfully deleted account ID: {}", request.getAccountId());
        responseObserver.onNext(Empty.getDefaultInstance());
        responseObserver.onCompleted();
    }


    private UserProfile findAuthenticatedUser() {
        String userId = securityUtil.getAuthenticatedUserId();
        log.debug("Finding user profile for authenticated user ID: {}", userId);
        return userProfileRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User profile not found for authenticated user"));
    }


    private Account findAccountByIdAndCheckOwnership(UUID accountId, String userId) {
        log.debug("Finding account by ID: {} and checking ownership for user ID: {}", accountId, userId);
        return accountRepository.findById(accountId)
                .filter(account -> account.getUserProfile().getUserId().equals(userId))
                .orElseThrow(() -> new NotFoundException("Account not found or access denied"));
    }


    private UUID parseUuid(String uuidString) {
        log.debug("Parsing UUID from string: {}", uuidString);
        try {
            return UUID.fromString(uuidString);
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Invalid Account ID format. Must be a valid UUID.");
        }
    }

    private BankName convertProtoToEntityEnum(BankNameProto protoEnum) {
        try {
            return BankName.valueOf(protoEnum.name());
        } catch (Exception e) {
            throw new IllegalArgumentException("Unknown bank enum: " + protoEnum.name(), e);
        }
    }
}