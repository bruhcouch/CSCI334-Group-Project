package com.example.accounts.bootstrap;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.example.accounts.model.Account;
import com.example.accounts.repository.AccountRepository;
import com.example.accounts.util.Role;
import com.example.accounts.util.Subscription;
import com.github.javafaker.Faker;

@Component
public class AccountSeeder implements CommandLineRunner {

    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;

    private static final Random random = new Random();
    private static final Faker faker = new Faker();
    private static final int NUMBER_OF_ENTRIES = 100;

    public AccountSeeder(AccountRepository accountRepository, PasswordEncoder passwordEncoder) {
        this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
    }

    private static Role randomRole() {
        int r = random.nextInt(100);

        if (r < 5) return Role.STAFF;
        else return Role.USER;
    }

    private static boolean randomStatus() {
        int r = random.nextInt(100);

        if (r < 10) return false;
        else return true;
    }

    private static LocalDateTime randomDate() {
        Date randomDate = faker.date().past(365, TimeUnit.DAYS);
        return randomDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();

    }

    @Override
    public void run(String... args) {
        seedPendingAccount("parking_staff_review", "parking.staff.review@uowmail.edu.au", Role.STAFF);
        seedPendingAccount("parking_admin_review", "parking.admin.review@uowmail.edu.au", Role.ADMIN);

        for (int i = 0; i < NUMBER_OF_ENTRIES; i++) {
            Account account = new Account();
            account.setUsername(faker.name().username());
            account.setEmail(faker.internet().emailAddress());
            account.setPassword(passwordEncoder.encode(faker.internet().password()));
            account.setRole(randomRole());
            account.setEnabled(randomStatus());
            account.setCreatedAt(randomDate());

            accountRepository.save(account);
        }

    }

    private void seedPendingAccount(String username, String email, Role role) {
        if (accountRepository.existsByEmail(email) || accountRepository.existsByUsername(username)) {
            return;
        }

        Account account = new Account();
        account.setUsername(username);
        account.setEmail(email);
        account.setPassword(passwordEncoder.encode("test123"));
        account.setRole(role);
        account.setEnabled(false);
        account.setSubscription(Subscription.FREE);
        account.setCreatedAt(LocalDateTime.now().minusDays(1));

        accountRepository.save(account);
    }

}
