package com.eduardo.accounts_service.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name="accounts")
@Getter @Setter @ToString
@NoArgsConstructor @AllArgsConstructor
public class AccountEntity extends BaseEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="account_number")
    private Long accountNumber;

    @Column(name="customer_id")
    private Long customerId;

    @Column(name="account_type")
    private Long accountType;

    @Column(name="branch_address")
    private Long branchAddress;

}
