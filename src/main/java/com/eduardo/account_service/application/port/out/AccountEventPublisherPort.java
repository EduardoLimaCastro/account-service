package com.eduardo.account_service.application.port.out;

import com.eduardo.account_service.application.dto.event.AccountCreditedEvent;
import com.eduardo.account_service.application.dto.event.AccountDebitedEvent;
import com.eduardo.account_service.application.dto.event.AccountRefundedEvent;
import com.eduardo.account_service.application.dto.event.CreditFailedEvent;
import com.eduardo.account_service.application.dto.event.DebitFailedEvent;

public interface AccountEventPublisherPort {
    void publishAccountDebited(AccountDebitedEvent event);
    void publishDebitFailed(DebitFailedEvent event);
    void publishAccountCredited(AccountCreditedEvent event);
    void publishCreditFailed(CreditFailedEvent event);
    void publishAccountRefunded(AccountRefundedEvent event);
}
