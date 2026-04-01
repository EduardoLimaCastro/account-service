package com.eduardo.account_service.domain.enums;

public enum AccountStatus {
    BLOCKED("Bloqueada"),
    CLOSED("Fechada"),
    ACTIVE("Ativa");

    private final String status;

    AccountStatus(String value) {this.status = value;}

    public String getStatusString() {
        return status;
    }

    public boolean canTransitionTo(AccountStatus target) {
        return switch (this) {
            case ACTIVE -> target == BLOCKED || target == CLOSED;
            case BLOCKED -> target == ACTIVE || target == CLOSED;
            case CLOSED -> false;
        };
    }
}
