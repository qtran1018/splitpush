package com.splitpush.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class SettlementDTO {
    @NotNull
    private Long payeeUserId; // User who receives the payment

    @NotNull
    private String tripGroupId;

    @NotNull
    @DecimalMin(value = "0.01")
    private BigDecimal amount;

    public SettlementDTO() {
    }

    public Long getPayeeUserId() {
        return payeeUserId;
    }

    public void setPayeeUserId(Long payeeUserId) {
        this.payeeUserId = payeeUserId;
    }

    public String getTripGroupId() {
        return tripGroupId;
    }

    public void setTripGroupId(String tripGroupId) {
        this.tripGroupId = tripGroupId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}

