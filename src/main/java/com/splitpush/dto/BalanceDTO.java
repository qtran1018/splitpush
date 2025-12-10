package com.splitpush.dto;

import java.math.BigDecimal;

public class BalanceDTO {
    private Long userId;
    private String username;
    private String name;
    private BigDecimal netBalance; // Positive = they owe you, Negative = you owe them
    // Breakdown of the net balance per group (key = group name)
    private java.util.Map<String, BigDecimal> groupBreakdown;
    // Detailed breakdown with stable group IDs to avoid name collisions
    private java.util.List<GroupBreakdownDTO> groupBreakdownDetails;

    public BalanceDTO() {
    }

    public BalanceDTO(Long userId, String username, String name, BigDecimal netBalance, java.util.Map<String, BigDecimal> groupBreakdown) {
        this.userId = userId;
        this.username = username;
        this.name = name;
        this.netBalance = netBalance;
        this.groupBreakdown = groupBreakdown;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BigDecimal getNetBalance() {
        return netBalance;
    }

    public void setNetBalance(BigDecimal netBalance) {
        this.netBalance = netBalance;
    }

    public java.util.Map<String, BigDecimal> getGroupBreakdown() {
        return groupBreakdown;
    }

    public void setGroupBreakdown(java.util.Map<String, BigDecimal> groupBreakdown) {
        this.groupBreakdown = groupBreakdown;
    }

    public java.util.List<GroupBreakdownDTO> getGroupBreakdownDetails() {
        return groupBreakdownDetails;
    }

    public void setGroupBreakdownDetails(java.util.List<GroupBreakdownDTO> groupBreakdownDetails) {
        this.groupBreakdownDetails = groupBreakdownDetails;
    }
}
