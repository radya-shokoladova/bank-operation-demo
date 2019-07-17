package com.shokoladova.bank_operations;

import java.sql.SQLException;
import java.util.UUID;

public interface BankAccountService {

    BankAccount create(String cardholderName, Integer initialBalance) throws SQLException;

    BankAccount get(UUID id) throws SQLException;

    BankAccount withdraw(UUID id, Integer amount) throws SQLException;

    BankAccount deposit(UUID id, Integer amount) throws SQLException;

    boolean transfer(TransferRequest transferRequest) throws SQLException;
}
