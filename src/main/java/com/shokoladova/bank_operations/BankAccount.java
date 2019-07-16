package com.shokoladova.bank_operations;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import lombok.Data;

import java.util.UUID;

@Data
@DatabaseTable
public class BankAccount {

    @DatabaseField(generatedId = true)
    private UUID id;

    @DatabaseField
    private String cardholderName;

    @DatabaseField
    private Integer balance;

    public BankAccount(String cardholderName, Integer initialBalance) {
        this.cardholderName = cardholderName;
        this.balance = initialBalance;
    }
}
