package com.shokoladova.bank_operations;

import spark.Request;

import java.util.UUID;

public interface BankAccountFacade {

    BankOperationResultDto create(Request req);

    BankOperationResultDto req(Request req);

    BankOperationResultDto withdraw(Request req);

    BankOperationResultDto deposit(Request req);

    BankOperationResultDto transfer(Request req);
}
