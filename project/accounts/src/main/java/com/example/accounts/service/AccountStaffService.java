package com.example.accounts.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.example.accounts.dto.response.AccountResponse;
import com.example.accounts.model.Account;
import com.example.accounts.repository.AccountRepository;
import com.example.accounts.specification.AccountSpecification;
import com.example.accounts.util.DateRange;
import com.example.accounts.util.Role;
import com.example.accounts.util.Subscription;

@Service
public class AccountStaffService {
    private final AccountRepository accountRepository;

    public AccountStaffService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    public List<AccountResponse> getAccounts(Boolean enabled, Role role, Subscription subscription, LocalDate startDate, LocalDate endDate) {
        LocalDateTime[] dateRange = DateRange.resolveRange(startDate, endDate);

        Specification<Account> specification =
            Specification.where(AccountSpecification.enabled(enabled))
                         .and(AccountSpecification.role(role))
                         .and(AccountSpecification.subscription(subscription))
                         .and(AccountSpecification.createdBetween(dateRange[0], dateRange[1]));

        return accountRepository.findAll(specification)
            .stream()
            .map(AccountResponse::new)
            .toList();
    }
}
