package com.example.accounts.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.example.accounts.dto.RegisterResult;
import com.example.accounts.dto.event.AccountDeletedEvent;
import com.example.accounts.dto.event.AccountUpdatedEvent;
import com.example.accounts.dto.request.RegisterRequest;
import com.example.accounts.dto.response.AccountResponse;
import com.example.accounts.model.Account;
import com.example.accounts.repository.AccountRepository;
import com.example.accounts.util.Role;


@Service
public class AccountAdminService {

    private final AccountRepository accountRepository;
    private final AccountService accountService;
    private final AccountProducerService accountProducerService;

    public AccountAdminService(
        AccountService accountService, 
        AccountRepository accountRepository,
        AccountProducerService accountProducerService) {

        this.accountService = accountService;
        this.accountRepository = accountRepository;
        this.accountProducerService = accountProducerService;
    }

    public RegisterResult registerAdmin(RegisterRequest registerRequest) {
        RegisterResult registerResult = accountService.register(registerRequest, Role.ADMIN, false);
        return registerResult;
    }

    public RegisterResult registerInitialAdmin(RegisterRequest registerRequest) {
        RegisterResult registerResult = accountService.register(registerRequest, Role.ADMIN, true);
        return registerResult;
    }

    public RegisterResult registerStaff(RegisterRequest registerRequest) {
        RegisterResult registerResult = accountService.register(registerRequest, Role.STAFF);
        return registerResult;
    }

    private Account getAccount(Long accountId) {
        return accountRepository.findById(accountId)
            .orElseThrow(() -> new RuntimeException("Account not found"));
    }

    public AccountResponse getAccountById(Long accountId) {
        Account account = getAccount(accountId);
        return new AccountResponse(account);
    }

    public List<AccountResponse> getAllAccounts() {
        return accountRepository.findAll().stream()
            .map(AccountResponse::new)
            .toList();
    }

    private void updateAccountEnabledStatus(Long accountId, boolean enabled) {
        Account account = getAccount(accountId);
        account.setEnabled(enabled);
        accountRepository.save(account);

        accountProducerService.publishAccountUpdatedEvent(new AccountUpdatedEvent(account));
    }

    public void enableAccount(Long accountId) {
        updateAccountEnabledStatus(accountId, true);
    }

    public void disableAccount(Long accountId) {
        updateAccountEnabledStatus(accountId, false);
    }


    public void deleteAccount(Long accountId) {
        accountRepository.deleteById(accountId);
        accountProducerService.publishAccountDeletedEvent(new AccountDeletedEvent(accountId));
    }

    public void deleteAccountByEmail(String email) {
        accountRepository.findByEmail(email).ifPresent(account -> {
            accountRepository.delete(account);
            accountProducerService.publishAccountDeletedEvent(new AccountDeletedEvent(account.getId()));
        });
    }

}
