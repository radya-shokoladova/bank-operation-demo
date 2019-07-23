package com.shokoladova.bank_operations;

import com.fasterxml.jackson.databind.ObjectMapper;
import spark.Request;

import java.util.Optional;
import java.util.UUID;

import static com.shokoladova.bank_operations.OperationStatus.NE_OK;
import static com.shokoladova.bank_operations.OperationStatus.OK;

public class BankAccountFacadeImpl implements BankAccountFacade {

    private final BankAccountService bankAccountService = new BankAccountServiceImpl();
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public BankOperationResultDto create(Request req) {
        try {
            String name = req.queryParams("cardholderName");
            Optional<Integer> balance = Optional.ofNullable(req.queryParams("balance"))
                    .map(Integer::parseInt);

            BankAccount bankAccount = balance.isPresent()?
                    bankAccountService.create(name, balance.get()):
                    bankAccountService.create(name, 0);

            return buildSuccessfullResult(bankAccount);
        } catch (Exception e) {
            return buildErrorResult(e);
        }
    }


    @Override
    public BankOperationResultDto get(Request req) {
        try {
            UUID id = UUID.fromString(req.queryParams("id"));
            BankAccount bankAccount = bankAccountService.get(id);

            return buildSuccessfullResult(bankAccount);
        } catch (Exception e) {
            return buildErrorResult(e);
        }
    }

    @Override
    public BankOperationResultDto withdraw(Request req) {
        try {
            UUID id = UUID.fromString(req.queryParams("id"));
            Integer amount = Integer.valueOf(req.queryParams("amount"));
            BankAccount bankAccount = bankAccountService.withdraw(id, amount);

            return buildSuccessfullResult(bankAccount);
        } catch (Exception e) {
            return buildErrorResult(e);
        }
    }

    @Override
    public BankOperationResultDto deposit(Request req) {
        try {
            UUID id = UUID.fromString(req.queryParams("id"));
            int amount = Integer.parseInt(req.queryParams("amount"));
            BankAccount bankAccount = bankAccountService.deposit(id, amount);

            return buildSuccessfullResult(bankAccount);
        } catch (Exception e) {
            return buildErrorResult(e);
        }
    }

    @Override
    public BankOperationResultDto transfer(Request req) {
        try {
            TransferRequest transferRequest = mapper.readValue(req.body(), TransferRequest.class);
            boolean trasnfered = bankAccountService.transfer(transferRequest);

            return buildSuccessfullResult(trasnfered);
        } catch (Exception e) {
            return buildErrorResult(e);
        }
    }

    private BankOperationResultDto buildSuccessfullResult(Object result) {
        return BankOperationResultDto.builder()
                .result(result)
                .status(OK)
                .build();
    }

    private BankOperationResultDto buildErrorResult(Exception e) {
        return BankOperationResultDto.builder()
                .status(NE_OK)
                .error(e)
                .build();
    }
}
