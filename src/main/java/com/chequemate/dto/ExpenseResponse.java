package com.chequemate.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class ExpenseResponse {
    private Long id;
    private String description;
    private BigDecimal amount;
    private BigDecimal totalAmount;
    private String splitType;
    private Long paidById;
    private Long paidByUserId;
    private String paidByName;
    private Long groupId;
    private LocalDateTime createdAt;
    private List<SplitResponse> splits;

    public ExpenseResponse() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getSplitType() {
        return splitType;
    }

    public void setSplitType(String splitType) {
        this.splitType = splitType;
    }

    public Long getPaidById() {
        return paidById;
    }

    public void setPaidById(Long paidById) {
        this.paidById = paidById;
        this.paidByUserId = paidById;
    }

    public Long getPaidByUserId() {
        return paidByUserId;
    }

    public void setPaidByUserId(Long paidByUserId) {
        this.paidByUserId = paidByUserId;
        this.paidById = paidByUserId;
    }

    public String getPaidByName() {
        return paidByName;
    }

    public void setPaidByName(String paidByName) {
        this.paidByName = paidByName;
    }

    public Long getGroupId() {
        return groupId;
    }

    public void setGroupId(Long groupId) {
        this.groupId = groupId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public List<SplitResponse> getSplits() {
        return splits;
    }

    public void setSplits(List<SplitResponse> splits) {
        this.splits = splits;
    }

    public static class SplitResponse {
        private Long userId;
        private String userName;
        private BigDecimal amountOwed;

        public SplitResponse() {}

        public SplitResponse(Long userId, String userName, BigDecimal amountOwed) {
            this.userId = userId;
            this.userName = userName;
            this.amountOwed = amountOwed;
        }

        public Long getUserId() {
            return userId;
        }

        public void setUserId(Long userId) {
            this.userId = userId;
        }

        public String getUserName() {
            return userName;
        }

        public void setUserName(String userName) {
            this.userName = userName;
        }

        public BigDecimal getAmountOwed() {
            return amountOwed;
        }

        public void setAmountOwed(BigDecimal amountOwed) {
            this.amountOwed = amountOwed;
        }
    }
}