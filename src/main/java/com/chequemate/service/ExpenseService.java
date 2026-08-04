package com.chequemate.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chequemate.dto.ExpenseRequest;
import com.chequemate.dto.ExpenseResponse;
import com.chequemate.entity.Expense;
import com.chequemate.entity.ExpenseSplit;
import com.chequemate.entity.Group;
import com.chequemate.entity.User;
import com.chequemate.repository.ExpenseRepository;
import com.chequemate.repository.GroupRepository;
import com.chequemate.repository.UserRepository;

@Service
public class ExpenseService {

    @Autowired
    private ExpenseRepository expenseRepository;

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private UserRepository userRepository;

    @Transactional
    public ExpenseResponse addExpense(Long groupId, ExpenseRequest request) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found with id: " + groupId));

        User paidBy = userRepository.findById(request.getPaidByUserId())
                .orElseThrow(() -> new RuntimeException("User not found with id: " + request.getPaidByUserId()));

        BigDecimal totalAmount = request.getTotalAmount() != null ? request.getTotalAmount() : request.getAmount();

        Expense expense = new Expense();
        expense.setDescription(request.getDescription());
        expense.setTotalAmount(totalAmount);
        expense.setSplitType(request.getSplitType() != null ? request.getSplitType() : "EQUAL");
        expense.setGroup(group);
        expense.setPaidBy(paidBy);
        expense.setCreatedAt(LocalDateTime.now());

        Set<User> members = group.getMembers();
        if (members != null && !members.isEmpty()) {
            if ("EQUAL".equalsIgnoreCase(expense.getSplitType())) {
                BigDecimal equalShare = totalAmount.divide(BigDecimal.valueOf(members.size()), 2, RoundingMode.HALF_UP);
                for (User member : members) {
                    expense.getSplits().add(new ExpenseSplit(expense, member, equalShare));
                }
            } else if (request.getMemberSplits() != null) {
                for (User member : members) {
                    Object valObj = request.getMemberSplits().get(member.getId());
                    if (valObj == null) {
                        valObj = request.getMemberSplits().get(member.getId().toString());
                    }

                    BigDecimal val = BigDecimal.ZERO;
                    if (valObj != null) {
                        try {
                            val = new BigDecimal(valObj.toString());
                        } catch (NumberFormatException ignored) {}
                    }

                    BigDecimal calculatedAmount;
                    if ("PERCENTAGE".equalsIgnoreCase(expense.getSplitType())) {
                        calculatedAmount = totalAmount.multiply(val).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                    } else {
                        calculatedAmount = val;
                    }

                    expense.getSplits().add(new ExpenseSplit(expense, member, calculatedAmount));
                }
            }
        }

        Expense saved = expenseRepository.save(expense);
        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ExpenseResponse> getGroupExpenses(Long groupId) {
        List<Expense> expenses = expenseRepository.findByGroupIdOrderByCreatedAtDesc(groupId);
        List<ExpenseResponse> responseList = new ArrayList<>();
        for (Expense e : expenses) {
            responseList.add(mapToResponse(e));
        }
        return responseList;
    }

    private ExpenseResponse mapToResponse(Expense expense) {
        ExpenseResponse response = new ExpenseResponse();
        response.setId(expense.getId());
        response.setDescription(expense.getDescription());
        response.setTotalAmount(expense.getTotalAmount());
        response.setAmount(expense.getTotalAmount());
        response.setSplitType(expense.getSplitType());
        response.setCreatedAt(expense.getCreatedAt());

        if (expense.getPaidBy() != null) {
            response.setPaidByUserId(expense.getPaidBy().getId());
            response.setPaidByName(expense.getPaidBy().getName());
        }

        return response;
    }
}