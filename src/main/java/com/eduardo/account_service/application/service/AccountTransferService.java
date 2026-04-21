package com.eduardo.account_service.application.service;

import com.eduardo.account_service.application.dto.event.AccountCreditedEvent;
import com.eduardo.account_service.application.dto.event.AccountDebitedEvent;
import com.eduardo.account_service.application.dto.event.AccountRefundedEvent;
import com.eduardo.account_service.application.dto.event.CreditAccountCommand;
import com.eduardo.account_service.application.dto.event.CreditFailedEvent;
import com.eduardo.account_service.application.dto.event.DebitFailedEvent;
import com.eduardo.account_service.application.dto.event.RefundAccountCommand;
import com.eduardo.account_service.application.dto.event.TransferRequestedEvent;
import com.eduardo.account_service.application.port.out.AccountEventPublisherPort;
import com.eduardo.account_service.application.port.out.AccountRepositoryPort;
import com.eduardo.account_service.domain.model.Account;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Service
@RequiredArgsConstructor
public class AccountTransferService {

    private static final Logger log = LoggerFactory.getLogger(AccountTransferService.class);

    private final AccountRepositoryPort repository;
    private final AccountEventPublisherPort eventPublisher;
    private final Clock clock;

    @Transactional
    public void debitSourceAccount(TransferRequestedEvent event) {
        log.info("Processing debit for transferId={}, sourceAccountId={}, amount={}",
                event.transferId(), event.sourceAccountId(), event.amount());

        Account account = repository.findById(event.sourceAccountId()).orElse(null);

        if (account == null) {
            log.warn("Source account not found: {}", event.sourceAccountId());
            eventPublisher.publishDebitFailed(new DebitFailedEvent(
                    event.transferId(), event.sourceAccountId(), "Source account not found"));
            return;
        }

        try {
            account.transferFunds(event.amount(), clock);
            repository.save(account);
            log.info("Debit successful for transferId={}", event.transferId());
            eventPublisher.publishAccountDebited(new AccountDebitedEvent(
                    event.transferId(), event.sourceAccountId(), event.amount()));
        } catch (IllegalStateException ex) {
            log.warn("Debit failed for transferId={}: {}", event.transferId(), ex.getMessage());
            eventPublisher.publishDebitFailed(new DebitFailedEvent(
                    event.transferId(), event.sourceAccountId(), ex.getMessage()));
        }
    }

    @Transactional
    public void creditTargetAccount(CreditAccountCommand command) {
        log.info("Processing credit for transferId={}, targetAccountId={}, amount={}",
                command.transferId(), command.targetAccountId(), command.amount());

        Account account = repository.findById(command.targetAccountId()).orElse(null);

        if (account == null) {
            log.warn("Target account not found: {}", command.targetAccountId());
            eventPublisher.publishCreditFailed(new CreditFailedEvent(
                    command.transferId(), command.targetAccountId(), "Target account not found"));
            return;
        }

        try {
            account.deposit(command.amount(), clock);
            repository.save(account);
            log.info("Credit successful for transferId={}", command.transferId());
            eventPublisher.publishAccountCredited(new AccountCreditedEvent(
                    command.transferId(), command.targetAccountId(), command.amount()));
        } catch (IllegalStateException ex) {
            log.warn("Credit failed for transferId={}: {}", command.transferId(), ex.getMessage());
            eventPublisher.publishCreditFailed(new CreditFailedEvent(
                    command.transferId(), command.targetAccountId(), ex.getMessage()));
        }
    }

    @Transactional
    public void refundSourceAccount(RefundAccountCommand command) {
        log.info("Processing refund for transferId={}, sourceAccountId={}, amount={}",
                command.transferId(), command.sourceAccountId(), command.amount());

        Account account = repository.findById(command.sourceAccountId()).orElse(null);

        if (account == null) {
            log.error("Source account not found for refund: {}", command.sourceAccountId());
            // Cannot refund — log critical error for manual intervention
            return;
        }

        try {
            account.deposit(command.amount(), clock);
            repository.save(account);
            log.info("Refund successful for transferId={}", command.transferId());
            eventPublisher.publishAccountRefunded(new AccountRefundedEvent(
                    command.transferId(), command.sourceAccountId(), command.amount()));
        } catch (IllegalStateException ex) {
            log.error("Refund failed for transferId={}: {}", command.transferId(), ex.getMessage(), ex);
            // Refund failure requires manual intervention — publish to DLQ via exception propagation
            throw ex;
        }
    }
}
