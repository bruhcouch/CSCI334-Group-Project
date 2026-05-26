package com.example.accounts.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.accounts.dto.RegisterResult;
import com.example.accounts.dto.request.RegisterRequest;
import com.example.accounts.dto.response.AccountResponse;
import com.example.accounts.dto.response.RegisterResponse;
import com.example.accounts.service.AccountAdminService;

@RestController
@RequestMapping("admin/accounts")
@PreAuthorize("hasRole('ADMIN')")
public class AccountAdminController {

    private final AccountAdminService accountAdminService;

    public AccountAdminController(AccountAdminService accountAdminService) {
        this.accountAdminService = accountAdminService;
    }

    @PostMapping("/staff")
    public ResponseEntity<RegisterResponse> registerStaff(@RequestBody RegisterRequest registerRequest) {
        RegisterResult registerResult = accountAdminService.registerStaff(registerRequest);

        RegisterResponse registerResponse = new RegisterResponse(
            registerResult.getId(),
            registerResult.getUsername(),
            registerResult.getEmail(),
            registerResult.getCreatedAt(),
            registerResult.getMessage());
            
        return new ResponseEntity<>(registerResponse, HttpStatus.CREATED);
    }

    @PostMapping("/admin")
    public ResponseEntity<RegisterResponse> registerAdmin(@RequestBody RegisterRequest registerRequest) {
        RegisterResult registerResult = accountAdminService.registerAdmin(registerRequest);

        RegisterResponse registerResponse = new RegisterResponse(
            registerResult.getId(),
            registerResult.getUsername(),
            registerResult.getEmail(),
            registerResult.getCreatedAt(),
            registerResult.getMessage());

        return new ResponseEntity<>(registerResponse, HttpStatus.CREATED);
    }

    @GetMapping
    public List<AccountResponse> getAllAccounts() {
        return accountAdminService.getAllAccounts();
    }

    @GetMapping("/{accountId}")
    public ResponseEntity<AccountResponse> getAccountById(@PathVariable Long accountId) {
        AccountResponse accountResponse = accountAdminService.getAccountById(accountId);
        return new ResponseEntity<>(accountResponse, HttpStatus.OK);
    }

    @PatchMapping("/{accountId}/enable")
    public ResponseEntity<Void> enableAccount(@PathVariable Long accountId) {
        accountAdminService.enableAccount(accountId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @PatchMapping("/{accountId}/disable")
    public ResponseEntity<Void> disableAccount(@PathVariable Long accountId) {
        accountAdminService.disableAccount(accountId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @DeleteMapping("/{accountId}")
    public ResponseEntity<Void> deleteAccount(@PathVariable Long accountId) {
        accountAdminService.deleteAccount(accountId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

}
