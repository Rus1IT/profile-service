package com.cashpilot.userservice.repository;

import com.cashpilot.userservice.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<Account, Long> {

}
