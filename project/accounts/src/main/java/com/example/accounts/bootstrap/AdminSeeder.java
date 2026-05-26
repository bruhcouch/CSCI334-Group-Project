package com.example.accounts.bootstrap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.example.accounts.dto.request.RegisterRequest;
import com.example.accounts.repository.AccountRepository;
import com.example.accounts.service.AccountAdminService;

@Component
public class AdminSeeder implements CommandLineRunner {

    @Value("${admin.username}")
    private String adminUsername;

    @Value("${admin.email}")
    private String adminEmail;

    @Value("${admin.password}")
    private String adminPassword;

    
    private final AccountRepository accountRepository;
    private final AccountAdminService accountAdminService;

    public AdminSeeder(AccountRepository accountRepository, AccountAdminService accountAdminService) {
        this.accountRepository = accountRepository;
        this.accountAdminService = accountAdminService;
    }

    @Override
    public void run(String... args) {

        if (!accountRepository.existsByEmail(adminEmail)) {
            RegisterRequest adminAccount = new RegisterRequest(adminUsername, adminEmail, adminPassword);
            accountAdminService.registerInitialAdmin(adminAccount);
        }
    }

}
