package vn.cozynest.billing.domain.repository;

import vn.cozynest.billing.domain.model.UserBankAccount;

import java.util.Optional;

public interface UserBankAccountRepository {
    Optional<UserBankAccount> findByUserId(String userId);
    UserBankAccount save(UserBankAccount account);
}

