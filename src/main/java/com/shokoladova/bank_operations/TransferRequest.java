package com.shokoladova.bank_operations;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class TransferRequest {

    private UUID sourceId;
    private UUID destinationId;
    private Integer amount;
}
