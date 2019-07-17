package com.shokoladova.bank_operations;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BankOperationResultDto {

    private OperationStatus status;
    private Object result;
    private Exception error;
}
