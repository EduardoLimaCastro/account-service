package com.eduardo.account_service.infrastructure.messaging;

import com.eduardo.account_service.application.dto.event.CreditAccountCommand;
import com.eduardo.account_service.application.dto.event.RefundAccountCommand;
import com.eduardo.account_service.application.dto.event.TransferRequestedEvent;
import com.eduardo.account_service.application.service.AccountTransferService;
import com.eduardo.account_service.infrastructure.repository.jpa.ProcessedEventJpaEntity;
import com.eduardo.account_service.infrastructure.repository.jpa.ProcessedEventJpaRepository;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import static com.eduardo.account_service.infrastructure.messaging.KafkaTopicConfig.*;

@Component
@RequiredArgsConstructor
public class AccountEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(AccountEventConsumer.class);

    private final AccountTransferService accountTransferService;
    private final ProcessedEventJpaRepository processedEventRepository;

    @KafkaListener(topics = TOPIC_TRANSFER_REQUESTED, groupId = "account-service-group",
            containerFactory = "transferRequestedFactory")
    @Transactional
    public void onTransferRequested(ConsumerRecord<String, TransferRequestedEvent> record,
                                    Acknowledgment ack) {
        String eventId = record.key();
        if (isAlreadyProcessed(eventId, TOPIC_TRANSFER_REQUESTED)) {
            log.warn("Duplicate event ignored: transferId={}, topic={}", eventId, TOPIC_TRANSFER_REQUESTED);
            ack.acknowledge();
            return;
        }
        try {
            accountTransferService.debitSourceAccount(record.value());
            markProcessed(eventId, TOPIC_TRANSFER_REQUESTED);
            ack.acknowledge();
        } catch (Exception ex) {
            log.error("Error processing transfer.requested for transferId={}: {}", eventId, ex.getMessage(), ex);
            throw ex;
        }
    }

    @KafkaListener(topics = TOPIC_CREDIT_ACCOUNT, groupId = "account-service-group",
            containerFactory = "creditAccountFactory")
    @Transactional
    public void onCreditAccount(ConsumerRecord<String, CreditAccountCommand> record,
                                Acknowledgment ack) {
        String eventId = record.key();
        if (isAlreadyProcessed(eventId, TOPIC_CREDIT_ACCOUNT)) {
            log.warn("Duplicate event ignored: transferId={}, topic={}", eventId, TOPIC_CREDIT_ACCOUNT);
            ack.acknowledge();
            return;
        }
        try {
            accountTransferService.creditTargetAccount(record.value());
            markProcessed(eventId, TOPIC_CREDIT_ACCOUNT);
            ack.acknowledge();
        } catch (Exception ex) {
            log.error("Error processing credit.account for transferId={}: {}", eventId, ex.getMessage(), ex);
            throw ex;
        }
    }

    @KafkaListener(topics = TOPIC_REFUND_ACCOUNT, groupId = "account-service-group",
            containerFactory = "refundAccountFactory")
    @Transactional
    public void onRefundAccount(ConsumerRecord<String, RefundAccountCommand> record,
                                Acknowledgment ack) {
        String eventId = record.key();
        if (isAlreadyProcessed(eventId, TOPIC_REFUND_ACCOUNT)) {
            log.warn("Duplicate event ignored: transferId={}, topic={}", eventId, TOPIC_REFUND_ACCOUNT);
            ack.acknowledge();
            return;
        }
        try {
            accountTransferService.refundSourceAccount(record.value());
            markProcessed(eventId, TOPIC_REFUND_ACCOUNT);
            ack.acknowledge();
        } catch (Exception ex) {
            log.error("Error processing refund.account for transferId={}: {}", eventId, ex.getMessage(), ex);
            throw ex;
        }
    }

    private boolean isAlreadyProcessed(String eventId, String topic) {
        return processedEventRepository.existsByIdEventIdAndIdTopic(eventId, topic);
    }

    private void markProcessed(String eventId, String topic) {
        processedEventRepository.save(new ProcessedEventJpaEntity(eventId, topic));
    }
}
