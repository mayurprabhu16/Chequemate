package com.chequemate.dto;

import java.math.BigDecimal;
import java.util.Map;

public class ExpenseRequest {

    private String description;
    private BigDecimal totalAmount;
    private BigDecimal amount;
    private String splitType;
    private Long paidByUserId;
    private Map<Object, Object> memberSplits;

    public ExpenseRequest() {}

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getSplitType() {
        return splitType;
    }

    public void setSplitType(String splitType) {
        this.splitType = splitType;
    }

    public Long getPaidByUserId() {
        return paidByUserId;
    }

    public void setPaidByUserId(Long paidByUserId) {
        this.paidByUserId = paidByUserId;
    }

    public Map<Object, Object> getMemberSplits() {
        return memberSplits;
    }

    public void setMemberSplits(Map<Object, Object> memberSplits) {
        this.memberSplits = memberSplits;
    }
}