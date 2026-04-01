package com.eduardo.account_service.domain.enums;

public enum AccountType {
    PERSONAL("Conta Pessoa Física"),
    ENTERPRISE("Conta Pessoa Jurídica");

    private final String type;

    AccountType(String value) {this.type = value;}

    public String getType() {return type;}
}
