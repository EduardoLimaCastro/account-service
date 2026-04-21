package com.eduardo.account_service.domain.model;

import com.eduardo.account_service.domain.enums.AccountStatus;
import com.eduardo.account_service.domain.enums.AccountType;
import com.eduardo.account_service.domain.exceptions.InvalidAccountStateTransitionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.*;

class AccountTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-01-01T10:00:00Z"), ZoneOffset.UTC);

    private Account activeAccount() {
        return Account.create("owner-1", "1", "agency-1",
                new BigDecimal("1000.00"),
                new BigDecimal("500.00"),
                new BigDecimal("2000.00"),
                AccountType.PERSONAL,
                CLOCK);
    }

    @Nested
    class Create {
        @Test
        void shouldCreateActiveAccount() {
            Account account = activeAccount();

            assertThat(account.getId()).isNotNull();
            assertThat(account.getStatus()).isEqualTo(AccountStatus.ACTIVE);
            assertThat(account.isFraudBlocked()).isFalse();
            assertThat(account.getAccountNumber()).isNull();
            assertThat(account.getCreatedAt()).isNotNull();
            assertThat(account.getUpdatedAt()).isNotNull();
        }
    }

    @Nested
    class StatusTransitions {
        @Test
        void shouldBlockActiveAccount() {
            Account account = activeAccount();
            account.block(CLOCK);
            assertThat(account.getStatus()).isEqualTo(AccountStatus.BLOCKED);
        }

        @Test
        void shouldCloseActiveAccount() {
            Account account = activeAccount();
            account.close(CLOCK);
            assertThat(account.getStatus()).isEqualTo(AccountStatus.CLOSED);
        }

        @Test
        void shouldActivateBlockedAccount() {
            Account account = activeAccount();
            account.block(CLOCK);
            account.activate(CLOCK);
            assertThat(account.getStatus()).isEqualTo(AccountStatus.ACTIVE);
        }

        @Test
        void shouldCloseBlockedAccount() {
            Account account = activeAccount();
            account.block(CLOCK);
            account.close(CLOCK);
            assertThat(account.getStatus()).isEqualTo(AccountStatus.CLOSED);
        }

        @Test
        void shouldNotTransitionFromClosed() {
            Account account = activeAccount();
            account.close(CLOCK);

            assertThatThrownBy(() -> account.activate(CLOCK))
                    .isInstanceOf(InvalidAccountStateTransitionException.class);
            assertThatThrownBy(() -> account.block(CLOCK))
                    .isInstanceOf(InvalidAccountStateTransitionException.class);
        }

        @Test
        void shouldUpdateTimestampOnTransition() {
            Clock later = Clock.fixed(Instant.parse("2026-01-01T11:00:00Z"), ZoneOffset.UTC);
            Account account = activeAccount();
            account.block(later);
            assertThat(account.getUpdatedAt()).isEqualTo(
                    java.time.LocalDateTime.now(later));
        }
    }

    @Nested
    class Deposit {
        @Test
        void shouldIncreaseBalance() {
            Account account = activeAccount();
            account.deposit(new BigDecimal("200.00"), CLOCK);
            assertThat(account.getBalance()).isEqualByComparingTo("1200.00");
        }

        @Test
        void shouldRejectNullAmount() {
            Account account = activeAccount();
            assertThatThrownBy(() -> account.deposit(null, CLOCK))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void shouldRejectZeroAmount() {
            Account account = activeAccount();
            assertThatThrownBy(() -> account.deposit(BigDecimal.ZERO, CLOCK))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void shouldRejectDepositOnBlockedAccount() {
            Account account = activeAccount();
            account.block(CLOCK);
            assertThatThrownBy(() -> account.deposit(new BigDecimal("100.00"), CLOCK))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("active");
        }

        @Test
        void shouldRejectDepositOnFraudBlockedAccount() {
            Account account = activeAccount();
            account.blockForFraud(CLOCK);
            assertThatThrownBy(() -> account.deposit(new BigDecimal("100.00"), CLOCK))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("fraud");
        }
    }

    @Nested
    class Withdraw {
        @Test
        void shouldDecreaseBalance() {
            Account account = activeAccount();
            account.withdraw(new BigDecimal("300.00"), CLOCK);
            assertThat(account.getBalance()).isEqualByComparingTo("700.00");
        }

        @Test
        void shouldAllowWithdrawWithinOverdraft() {
            Account account = activeAccount();
            account.withdraw(new BigDecimal("1400.00"), CLOCK); // balance 1000 + overdraft 500 = 1500
            assertThat(account.getBalance()).isEqualByComparingTo("-400.00");
        }

        @Test
        void shouldRejectWithdrawExceedingAvailableFunds() {
            Account account = activeAccount();
            assertThatThrownBy(() -> account.withdraw(new BigDecimal("1600.00"), CLOCK))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Insufficient funds");
        }

        @Test
        void shouldRejectNullAmount() {
            Account account = activeAccount();
            assertThatThrownBy(() -> account.withdraw(null, CLOCK))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void shouldRejectWithdrawOnBlockedAccount() {
            Account account = activeAccount();
            account.block(CLOCK);
            assertThatThrownBy(() -> account.withdraw(new BigDecimal("100.00"), CLOCK))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        void shouldRejectWithdrawOnFraudBlockedAccount() {
            Account account = activeAccount();
            account.blockForFraud(CLOCK);
            assertThatThrownBy(() -> account.withdraw(new BigDecimal("100.00"), CLOCK))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("fraud");
        }
    }

    @Nested
    class TransferFunds {
        @Test
        void shouldWithdrawAmountWithinTransferLimit() {
            Account account = activeAccount();
            account.transferFunds(new BigDecimal("500.00"), CLOCK);
            assertThat(account.getBalance()).isEqualByComparingTo("500.00");
        }

        @Test
        void shouldRejectAmountExceedingTransferLimit() {
            Account account = activeAccount();
            assertThatThrownBy(() -> account.transferFunds(new BigDecimal("2001.00"), CLOCK))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("transfer limit");
        }

        @Test
        void shouldRejectNullAmount() {
            Account account = activeAccount();
            assertThatThrownBy(() -> account.transferFunds(null, CLOCK))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void shouldRejectTransferOnFraudBlockedAccount() {
            Account account = activeAccount();
            account.blockForFraud(CLOCK);
            assertThatThrownBy(() -> account.transferFunds(new BigDecimal("100.00"), CLOCK))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("fraud");
        }

        @Test
        void shouldRejectTransferOnBlockedAccount() {
            Account account = activeAccount();
            account.block(CLOCK);
            assertThatThrownBy(() -> account.transferFunds(new BigDecimal("100.00"), CLOCK))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    @Nested
    class FraudBlock {
        @Test
        void shouldBlockForFraud() {
            Account account = activeAccount();
            account.blockForFraud(CLOCK);
            assertThat(account.isFraudBlocked()).isTrue();
        }

        @Test
        void shouldUnblockForFraud() {
            Account account = activeAccount();
            account.blockForFraud(CLOCK);
            account.unblockForFraud(CLOCK);
            assertThat(account.isFraudBlocked()).isFalse();
        }
    }

    @Nested
    class Equality {
        @Test
        void accountsWithSameIdAreEqual() {
            Account a = activeAccount();
            Account b = Account.reconstitute(
                    a.getId(), "other-owner", "00001", "2", "agency-2",
                    AccountStatus.BLOCKED, BigDecimal.TEN, BigDecimal.ONE, BigDecimal.TEN,
                    AccountType.ENTERPRISE, false,
                    a.getCreatedAt(), a.getUpdatedAt(), 0L);
            assertThat(a).isEqualTo(b);
        }

        @Test
        void accountsWithDifferentIdsAreNotEqual() {
            Account a = activeAccount();
            Account b = activeAccount();
            assertThat(a).isNotEqualTo(b);
        }
    }
}
