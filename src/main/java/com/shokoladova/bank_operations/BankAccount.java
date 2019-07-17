package com.shokoladova.bank_operations;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import lombok.Data;

import java.util.UUID;

@Data
@DatabaseTable(tableName = "ACCOUNT")
public class BankAccount {

    @DatabaseField(generatedId = true)
    private UUID id;

    @DatabaseField
    private String cardholderName;

    @DatabaseField(columnDefinition = "BIGINT CHECK BALANCE >= 0")
    private Integer balance;

    public BankAccount(String cardholderName, Integer initialBalance) {
        this.cardholderName = cardholderName;
        this.balance = initialBalance;
    }
}
