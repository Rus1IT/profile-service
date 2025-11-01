package com.cashpilot.userservice.repository;

import com.cashpilot.account.proto.BankNameProto;
import com.cashpilot.userservice.entity.Account;
import com.cashpilot.userservice.enums.BankName;
import com.cashpilot.userservice.entity.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AccountRepository extends JpaRepository<Account, UUID> {
    List<Account> findAllByUserProfile_UserId(String userId);

    boolean existsByUserProfileAndBankName(UserProfile userProfile, BankName bankName);
}