package com.splitpush.dto;

import java.math.BigDecimal;

public class GroupBreakdownDTO {
    private String groupId;
    private String groupName;
    private BigDecimal amount;

    public GroupBreakdownDTO() {
    }

    public GroupBreakdownDTO(String groupId, String groupName, BigDecimal amount) {
        this.groupId = groupId;
        this.groupName = groupName;
        this.amount = amount;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}

