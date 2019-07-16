package com.shokoladova.bank_operations;

public class BankAccountNotFountException extends RuntimeException {
    public BankAccountNotFountException(String s) {
        super(s);
    }
}
