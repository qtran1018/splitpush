package com.splitpush.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.Map;

public class ExpenseDTO {
    @NotBlank
    private String description;

    @NotNull
    @DecimalMin(value = "0.01")
    private BigDecimal amount;

    @NotNull
    private Long paidByUserId;

    @NotNull
    private String tripGroupId;

    // Map of userId to amount they owe
    private Map<Long, BigDecimal> participantAmounts;

    public ExpenseDTO() {
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public Long getPaidByUserId() {
        return paidByUserId;
    }

    public void setPaidByUserId(Long paidByUserId) {
        this.paidByUserId = paidByUserId;
    }

    public String getTripGroupId() {
        return tripGroupId;
    }

    public void setTripGroupId(String tripGroupId) {
        this.tripGroupId = tripGroupId;
    }

    public Map<Long, BigDecimal> getParticipantAmounts() {
        return participantAmounts;
    }

    public void setParticipantAmounts(Map<Long, BigDecimal> participantAmounts) {
        this.participantAmounts = participantAmounts;
    }
}
