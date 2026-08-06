package com.chequemate.entity;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "expense_splits")
public class ExpenseSplit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expense_id")
    @JsonBackReference
    private Expense expense;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private User user;

    private BigDecimal amount;

    @Column(name = "amount_owed")
    private BigDecimal amountOwed;

    private Double percentage;

    public ExpenseSplit() {}

    public ExpenseSplit(User user, BigDecimal amount) {
        this.user = user;
        this.amount = amount;
        this.amountOwed = amount;
    }

    public ExpenseSplit(Expense expense, User user, BigDecimal amount) {
        this.expense = expense;
        this.user = user;
        this.amount = amount;
        this.amountOwed = amount;
    }

    // --- GETTERS & SETTERS ---

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Expense getExpense() {
        return expense;
    }

    public void setExpense(Expense expense) {
        this.expense = expense;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public BigDecimal getAmountOwed() {
        return amountOwed;
    }

    public void setAmountOwed(BigDecimal amountOwed) {
        this.amountOwed = amountOwed;
    }

    public Double getPercentage() {
        return percentage;
    }

    public void setPercentage(Double percentage) {
        this.percentage = percentage;
    }

    @Override
    public String toString() {
        return "ExpenseSplit{" +
                "id=" + id +
                ", amount=" + amount +
                ", amountOwed=" + amountOwed +
                '}';
    }
}