package com.shokoladova.bank_operations;

import java.util.UUID;

public interface BankAccountService {

    BankAccount create(String cardholderName, Integer initialBalance);

    BankAccount create(String cardholderName);

    BankAccount get(UUID id);

    BankAccount withdraw(UUID id, Integer amount);

    BankAccount deposit(UUID id, Integer amount);

    boolean transfer(TransferRequest transferRequest);
}
