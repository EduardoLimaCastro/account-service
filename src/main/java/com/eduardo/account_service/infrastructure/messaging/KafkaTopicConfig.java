package com.eduardo.account_service.infrastructure.messaging;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    // Topics consumed by account-service
    public static final String TOPIC_TRANSFER_REQUESTED = "transfer.requested";
    public static final String TOPIC_CREDIT_ACCOUNT     = "credit.account";
    public static final String TOPIC_REFUND_ACCOUNT     = "refund.account";

    // Topics published by account-service
    public static final String TOPIC_ACCOUNT_DEBITED  = "account.debited";
    public static final String TOPIC_DEBIT_FAILED     = "debit.failed";
    public static final String TOPIC_ACCOUNT_CREDITED = "account.credited";
    public static final String TOPIC_CREDIT_FAILED    = "credit.failed";
    public static final String TOPIC_ACCOUNT_REFUNDED = "account.refunded";

    // DLQ
    public static final String TOPIC_ACCOUNT_EVENTS_DLQ = "account.events.dlq";

    @Bean
    public NewTopic transferRequestedTopic() {
        return TopicBuilder.name(TOPIC_TRANSFER_REQUESTED).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic creditAccountTopic() {
        return TopicBuilder.name(TOPIC_CREDIT_ACCOUNT).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic refundAccountTopic() {
        return TopicBuilder.name(TOPIC_REFUND_ACCOUNT).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic accountDebitedTopic() {
        return TopicBuilder.name(TOPIC_ACCOUNT_DEBITED).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic debitFailedTopic() {
        return TopicBuilder.name(TOPIC_DEBIT_FAILED).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic accountCreditedTopic() {
        return TopicBuilder.name(TOPIC_ACCOUNT_CREDITED).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic creditFailedTopic() {
        return TopicBuilder.name(TOPIC_CREDIT_FAILED).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic accountRefundedTopic() {
        return TopicBuilder.name(TOPIC_ACCOUNT_REFUNDED).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic accountEventsDlqTopic() {
        return TopicBuilder.name(TOPIC_ACCOUNT_EVENTS_DLQ).partitions(3).replicas(1).build();
    }
}
