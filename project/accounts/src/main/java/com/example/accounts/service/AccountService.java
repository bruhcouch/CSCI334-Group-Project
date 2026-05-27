package com.example.accounts.service;

import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.example.accounts.dto.AuthResult;
import com.example.accounts.dto.RegisterResult;
import com.example.accounts.dto.event.AccountCreatedEvent;
import com.example.accounts.dto.event.AccountUpdatedEvent;
import com.example.accounts.dto.event.LoginFailedEvent;
import com.example.accounts.dto.event.LoginSucceededEvent;
import com.example.accounts.dto.event.TokenIssuedEvent;
import com.example.accounts.dto.request.LoginRequest;
import com.example.accounts.dto.request.RegisterRequest;
import com.example.accounts.dto.request.UpdateRequest;
import com.example.accounts.dto.response.AccountResponse;
import com.example.accounts.model.Account;
import com.example.accounts.repository.AccountRepository;
import com.example.accounts.security.JwtService;
import com.example.accounts.util.Role;
import com.example.accounts.util.Subscription;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final AccountProducerService accountEventProducer;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AccountService(
        AccountRepository accountRepository,
        AccountProducerService accountEventProducer,
        PasswordEncoder passwordEncoder,
        JwtService jwtService) {

        this.accountRepository = accountRepository;
        this.accountEventProducer = accountEventProducer;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public RegisterResult register(RegisterRequest registerRequest) {
        return register(registerRequest, Role.USER);
    }

    public RegisterResult register(RegisterRequest registerRequest, Role role) {
        return register(registerRequest, role, true);
    }

    public RegisterResult register(RegisterRequest registerRequest, Role role, boolean enabled) {

        if (accountRepository.existsByEmail(registerRequest.getEmail())) {
            throw new IllegalArgumentException("Email already in use");
        }
        if (accountRepository.existsByUsername(registerRequest.getUsername())) {
            throw new IllegalArgumentException("Username already in use");
        }

        Account account = new Account();
        account.setUsername(registerRequest.getUsername());
        account.setEmail(registerRequest.getEmail());
        account.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
        account.setRole(role);
        account.setEnabled(enabled);
        account.setSubscription(Subscription.FREE);
        account = accountRepository.save(account);

        AccountCreatedEvent event = new AccountCreatedEvent(account);
        accountEventProducer.publishAccountCreatedEvent(event);

        String token = jwtService.generateToken(account);

        TokenIssuedEvent tokenEvent = new TokenIssuedEvent(
            account.getId(),
            jwtService.extractIssuedAt(token),
            jwtService.extractExpiration(token)
        );

        accountEventProducer.publishTokenIssuedEvent(tokenEvent);

        return new RegisterResult(account, "Account created successfully", token);
    }

    public AuthResult authenticate(LoginRequest loginRequest) {

        Optional<Account> optionalAccount = accountRepository.findByEmail(loginRequest.getEmail());

        if (optionalAccount.isEmpty()) {
            LoginFailedEvent event = new LoginFailedEvent(loginRequest.getEmail(), "No account found with email: " + loginRequest.getEmail());

            accountEventProducer.publishLoginFailedEvent(event);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }

        Account account = optionalAccount.get();

        if (!account.isEnabled()) {
            LoginFailedEvent event = new LoginFailedEvent(account, "Account not approved");
            accountEventProducer.publishLoginFailedEvent(event);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Account is waiting for admin approval");
        }

        boolean matches = passwordEncoder.matches(
            loginRequest.getPassword(), 
            account.getPassword()
        );

        if (!matches) {
            LoginFailedEvent event = new LoginFailedEvent(account, "Invalid password");

            accountEventProducer.publishLoginFailedEvent(event);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }

        String token = jwtService.generateToken(account);
        
        TokenIssuedEvent tokenEvent = new TokenIssuedEvent(
            account.getId(),
            jwtService.extractIssuedAt(token),
            jwtService.extractExpiration(token)
        );

        accountEventProducer.publishTokenIssuedEvent(tokenEvent);

        LoginSucceededEvent loginEvent = new LoginSucceededEvent(account);
        accountEventProducer.publishLoginSucceededEvent(loginEvent);

        return new AuthResult(account, token);
    }

    public AccountResponse getAccount(String email) {
        Account account = accountRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("Account not found"));

        return new AccountResponse(account);
    }

    public Subscription getSubscription(Long accountId) {
        Account account = accountRepository.findById(accountId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found"));

        return account.getSubscription() == null ? Subscription.FREE : account.getSubscription();
    }


    public void update(UpdateRequest updateRequest, String email) {
        Account account = accountRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("Account not found"));

        if (updateRequest.getUsername() != null)
            account.setUsername(updateRequest.getUsername());

        if (updateRequest.getEmail() != null)
            account.setEmail(updateRequest.getEmail());

        if (updateRequest.getPassword() != null)
            account.setPassword(passwordEncoder.encode(updateRequest.getPassword()));

        accountRepository.save(account);

        AccountUpdatedEvent event = new AccountUpdatedEvent(account);
        accountEventProducer.publishAccountUpdatedEvent(event);
    }

    private void updateSubscription(String email, Subscription subscription) {
        Account account = accountRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("Account not found"));

        account.setSubscription(subscription);
        accountRepository.save(account);

        AccountUpdatedEvent event = new AccountUpdatedEvent(account);
        accountEventProducer.publishAccountUpdatedEvent(event);
    }

    

    public void upgradeSubscription(String email) {
        updateSubscription(email, Subscription.PREMIUM);
    }


    public void downgradeSubscription(String email) {
        updateSubscription(email, Subscription.FREE);
    }
}
