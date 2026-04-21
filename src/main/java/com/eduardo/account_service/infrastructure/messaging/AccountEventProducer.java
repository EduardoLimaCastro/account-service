package com.eduardo.account_service.infrastructure.messaging;

import com.eduardo.account_service.application.dto.event.AccountCreditedEvent;
import com.eduardo.account_service.application.dto.event.AccountDebitedEvent;
import com.eduardo.account_service.application.dto.event.AccountRefundedEvent;
import com.eduardo.account_service.application.dto.event.CreditFailedEvent;
import com.eduardo.account_service.application.dto.event.DebitFailedEvent;
import com.eduardo.account_service.application.port.out.AccountEventPublisherPort;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import static com.eduardo.account_service.infrastructure.messaging.KafkaTopicConfig.*;

@Component
@RequiredArgsConstructor
public class AccountEventProducer implements AccountEventPublisherPort {

    private static final Logger log = LoggerFactory.getLogger(AccountEventProducer.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    public void publishAccountDebited(AccountDebitedEvent event) {
        log.info("Publishing account.debited for transferId={}", event.transferId());
        kafkaTemplate.send(TOPIC_ACCOUNT_DEBITED, event.transferId().toString(), event);
    }

    @Override
    public void publishDebitFailed(DebitFailedEvent event) {
        log.warn("Publishing debit.failed for transferId={}, reason={}", event.transferId(), event.reason());
        kafkaTemplate.send(TOPIC_DEBIT_FAILED, event.transferId().toString(), event);
    }

    @Override
    public void publishAccountCredited(AccountCreditedEvent event) {
        log.info("Publishing account.credited for transferId={}", event.transferId());
        kafkaTemplate.send(TOPIC_ACCOUNT_CREDITED, event.transferId().toString(), event);
    }

    @Override
    public void publishCreditFailed(CreditFailedEvent event) {
        log.warn("Publishing credit.failed for transferId={}, reason={}", event.transferId(), event.reason());
        kafkaTemplate.send(TOPIC_CREDIT_FAILED, event.transferId().toString(), event);
    }

    @Override
    public void publishAccountRefunded(AccountRefundedEvent event) {
        log.info("Publishing account.refunded for transferId={}", event.transferId());
        kafkaTemplate.send(TOPIC_ACCOUNT_REFUNDED, event.transferId().toString(), event);
    }
}
