package com.chequemate.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Entity
@Table(name = "expense_splits")
public class ExpenseSplit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "expense_id", nullable = false)
    @JsonIgnoreProperties({"splits", "group", "paidBy"})
    private Expense expense;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnoreProperties({"groups", "expenses", "password"})
    private User user;

    @Column(name = "amount_owed", precision = 10, scale = 2)
    private BigDecimal amountOwed;

    public ExpenseSplit() {}

    public ExpenseSplit(Expense expense, User user, BigDecimal amountOwed) {
        this.expense = expense;
        this.user = user;
        this.amountOwed = amountOwed != null ? amountOwed.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
    }

    public ExpenseSplit(Expense expense, User user, double amountOwed) {
        this(expense, user, BigDecimal.valueOf(amountOwed));
    }

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

    public BigDecimal getAmountOwed() {
        return amountOwed;
    }

    public void setAmountOwed(BigDecimal amountOwed) {
        this.amountOwed = amountOwed;
    }

    public BigDecimal getAmount() {
        return amountOwed;
    }

    public void setAmount(BigDecimal amount) {
        this.amountOwed = amount;
    }
}