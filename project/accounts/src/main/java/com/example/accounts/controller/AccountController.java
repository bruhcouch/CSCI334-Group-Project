package com.example.accounts.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.accounts.dto.AuthResult;
import com.example.accounts.dto.RegisterResult;
import com.example.accounts.dto.request.LoginRequest;
import com.example.accounts.dto.request.RegisterRequest;
import com.example.accounts.dto.request.UpdateRequest;
import com.example.accounts.dto.response.AccountResponse;
import com.example.accounts.dto.response.AuthResponse;
import com.example.accounts.dto.response.RegisterResponse;
import com.example.accounts.service.AccountService;
import com.example.accounts.util.Role;

import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@RequestBody RegisterRequest registerRequest, HttpServletResponse response) {
        RegisterResult registerResult = accountService.register(registerRequest);

        ResponseCookie cookie = ResponseCookie.from("jwt", registerResult.getToken())
            .httpOnly(true)
            .secure(false)
            .path("/")
            .maxAge(7 * 24 * 60 * 60)
            .sameSite("Lax")
            .build();

        RegisterResponse registerResponse = new RegisterResponse(
            registerResult.getId(),
            registerResult.getUsername(),
            registerResult.getEmail(),
            registerResult.getCreatedAt(),
            registerResult.getMessage());

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .header("Set-Cookie", cookie.toString())
            .body(registerResponse);
    }

    @PostMapping("/register/staff")
    public ResponseEntity<RegisterResponse> registerStaff(@RequestBody RegisterRequest registerRequest) {
        RegisterResult registerResult = accountService.register(registerRequest, Role.STAFF, false);

        RegisterResponse registerResponse = new RegisterResponse(
            registerResult.getId(),
            registerResult.getUsername(),
            registerResult.getEmail(),
            registerResult.getCreatedAt(),
            "Staff account submitted for approval");

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(registerResponse);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest loginRequest, HttpServletResponse response) {
        AuthResult authResult = accountService.authenticate(loginRequest);

        ResponseCookie cookie = ResponseCookie.from("jwt", authResult.getToken())
            .httpOnly(true)
            .secure(false)
            .path("/")
            .maxAge(7 * 24 * 60 * 60)
            .sameSite("Lax")
            .build();

        AuthResponse authResponse = new AuthResponse(
            authResult.getId(),
            authResult.getUsername(),
            authResult.getEmail(),
            authResult.getRole());

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .header("Set-Cookie", cookie.toString())
            .body(authResponse);
    }

    @GetMapping()
    public ResponseEntity<AccountResponse> getAccount() {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();

        AccountResponse accountResponse = accountService.getAccount(email);
        return new ResponseEntity<>(accountResponse, HttpStatus.OK);

    }

    @PatchMapping()
    public ResponseEntity<Void> update(@RequestBody UpdateRequest updateRequest) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();

        accountService.update(updateRequest, email);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @PatchMapping("/subscription/upgrade")
    public ResponseEntity<Void> upgradeSubscription() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
    
        accountService.upgradeSubscription(email);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @PatchMapping("/subscription/downgrade")
    public ResponseEntity<Void> downgradeSubscription() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();

        accountService.downgradeSubscription(email);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
