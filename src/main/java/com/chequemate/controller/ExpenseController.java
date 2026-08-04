package com.chequemate.controller;

import com.chequemate.entity.Expense;
import com.chequemate.entity.ExpenseSplit;
import com.chequemate.entity.Group;
import com.chequemate.entity.User;
import com.chequemate.repository.ExpenseRepository;
import com.chequemate.repository.GroupRepository;
import com.chequemate.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/groups")
@CrossOrigin(origins = "*")
public class ExpenseController {

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ExpenseRepository expenseRepository;

    @GetMapping("/{groupId}/expenses")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getGroupExpenses(@PathVariable Long groupId) {
        try {
            List<Expense> expenses = expenseRepository.findByGroupIdOrderByCreatedAtDesc(groupId);
            List<Map<String, Object>> response = new ArrayList<>();

            for (Expense e : expenses) {
                Map<String, Object> map = new HashMap<>();
                map.put("id", e.getId());
                map.put("description", e.getDescription());
                map.put("totalAmount", e.getTotalAmount());
                map.put("splitType", e.getSplitType());
                map.put("createdAt", e.getCreatedAt());
                map.put("paidByUserId", e.getPaidBy() != null ? e.getPaidBy().getId() : null);
                map.put("paidByName", e.getPaidBy() != null ? e.getPaidBy().getName() : "Unknown");
                response.add(map);
            }

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of("message", "Error fetching expenses: " + e.getMessage()));
        }
    }

    @PostMapping("/{groupId}/expenses")
    @Transactional
    public ResponseEntity<?> addExpense(@PathVariable Long groupId, @RequestBody Map<String, Object> payload) {
        try {
            Group group = groupRepository.findById(groupId).orElse(null);
            if (group == null) {
                return ResponseEntity.status(404).body(Map.of("message", "Group not found with id: " + groupId));
            }

            // Extract user making the request
            Long activeUserId = null;
            if (payload.get("activeUserId") != null) {
                activeUserId = Long.valueOf(payload.get("activeUserId").toString());
            }

            // ADMIN LEDGER MODE GUARD: Restrict expense creation strictly to group creator
            if ("ADMIN_ONLY".equalsIgnoreCase(group.getMode())) {
                Long creatorId = group.getCreatedBy() != null ? group.getCreatedBy().getId() : null;

                if (activeUserId == null || !activeUserId.equals(creatorId)) {
                    return ResponseEntity.status(403).body(Map.of(
                        "message", "Admin Ledger Mode: Only the group creator can add expenses to this group."
                    ));
                }
            }

            String description = (String) payload.get("description");

            Object totalAmtObj = payload.get("totalAmount") != null ? payload.get("totalAmount") : payload.get("amount");
            if (totalAmtObj == null) {
                return ResponseEntity.badRequest().body(Map.of("message", "Total amount is required"));
            }
            BigDecimal totalAmount = new BigDecimal(totalAmtObj.toString());

            String splitType = (String) payload.getOrDefault("splitType", "EQUAL");

            // Extract the selected payer
            Long paidByUserId = null;
            if (payload.get("paidByUserId") != null) {
                paidByUserId = Long.valueOf(payload.get("paidByUserId").toString());
            }

            if (paidByUserId == null) {
                return ResponseEntity.badRequest().body(Map.of("message", "Payer (paidByUserId) must be selected"));
            }

            User paidBy = userRepository.findById(paidByUserId).orElse(null);
            if (paidBy == null) {
                return ResponseEntity.badRequest().body(Map.of("message", "Selected payer not found in database"));
            }

            Expense expense = new Expense();
            expense.setDescription(description);
            expense.setTotalAmount(totalAmount);
            expense.setSplitType(splitType);
            expense.setGroup(group);
            expense.setPaidBy(paidBy);
            expense.setCreatedAt(LocalDateTime.now());

            Set<User> members = group.getMembers();
            if (members != null && !members.isEmpty()) {
                if ("EQUAL".equalsIgnoreCase(splitType)) {
                    BigDecimal splitAmount = totalAmount.divide(BigDecimal.valueOf(members.size()), 2, RoundingMode.HALF_UP);
                    for (User member : members) {
                        expense.getSplits().add(new ExpenseSplit(expense, member, splitAmount));
                    }
                } else if (payload.get("memberSplits") instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> customSplits = (Map<String, Object>) payload.get("memberSplits");
                    for (User member : members) {
                        Object splitVal = customSplits.get(member.getId().toString());
                        BigDecimal val = splitVal != null ? new BigDecimal(splitVal.toString()) : BigDecimal.ZERO;

                        BigDecimal calculatedOwed = "PERCENTAGE".equalsIgnoreCase(splitType)
                                ? totalAmount.multiply(val).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
                                : val;

                        expense.getSplits().add(new ExpenseSplit(expense, member, calculatedOwed));
                    }
                }
            }

            Expense savedExpense = expenseRepository.saveAndFlush(expense);

            Map<String, Object> response = new HashMap<>();
            response.put("id", savedExpense.getId());
            response.put("description", savedExpense.getDescription());
            response.put("totalAmount", savedExpense.getTotalAmount());
            response.put("splitType", savedExpense.getSplitType());
            response.put("paidByUserId", savedExpense.getPaidBy().getId());
            response.put("paidByName", savedExpense.getPaidBy().getName());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of("message", "Failed to add expense: " + e.getMessage()));
        }
    }

    @GetMapping("/{groupId}/balances")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getSimplifiedBalances(@PathVariable Long groupId) {
        try {
            List<Expense> expenses = expenseRepository.findByGroupIdOrderByCreatedAtDesc(groupId);

            Map<Long, BigDecimal> netBalances = new HashMap<>();
            Map<Long, String> userNameMap = new HashMap<>();

            for (Expense expense : expenses) {
                User payer = expense.getPaidBy();
                if (payer == null) continue;

                userNameMap.put(payer.getId(), payer.getName());

                for (ExpenseSplit split : expense.getSplits()) {
                    User debtor = split.getUser();
                    if (debtor == null) continue;

                    userNameMap.put(debtor.getId(), debtor.getName());

                    BigDecimal owed = split.getAmountOwed() != null ? split.getAmountOwed() : BigDecimal.ZERO;

                    netBalances.put(debtor.getId(), netBalances.getOrDefault(debtor.getId(), BigDecimal.ZERO).subtract(owed));
                    netBalances.put(payer.getId(), netBalances.getOrDefault(payer.getId(), BigDecimal.ZERO).add(owed));
                }
            }

            List<Long> debtors = new ArrayList<>();
            List<Long> creditors = new ArrayList<>();

            for (Map.Entry<Long, BigDecimal> entry : netBalances.entrySet()) {
                if (entry.getValue().compareTo(new BigDecimal("-0.01")) < 0) {
                    debtors.add(entry.getKey());
                } else if (entry.getValue().compareTo(new BigDecimal("0.01")) > 0) {
                    creditors.add(entry.getKey());
                }
            }

            List<Map<String, Object>> balancesList = new ArrayList<>();
            int dIdx = 0;
            int cIdx = 0;

            while (dIdx < debtors.size() && cIdx < creditors.size()) {
                Long debtorId = debtors.get(dIdx);
                Long creditorId = creditors.get(cIdx);

                BigDecimal debtAmount = netBalances.get(debtorId).abs();
                BigDecimal creditAmount = netBalances.get(creditorId);

                BigDecimal settledAmount = debtAmount.min(creditAmount);

                if (settledAmount.compareTo(new BigDecimal("0.01")) >= 0) {
                    Map<String, Object> item = new HashMap<>();
                    item.put("fromUserId", debtorId);
                    item.put("fromUserName", userNameMap.getOrDefault(debtorId, "Member"));
                    item.put("toUserId", creditorId);
                    item.put("toUserName", userNameMap.getOrDefault(creditorId, "Member"));
                    item.put("amount", settledAmount);
                    balancesList.add(item);
                }

                netBalances.put(debtorId, netBalances.get(debtorId).add(settledAmount));
                netBalances.put(creditorId, netBalances.get(creditorId).subtract(settledAmount));

                if (netBalances.get(debtorId).abs().compareTo(new BigDecimal("0.01")) < 0) {
                    dIdx++;
                }
                if (netBalances.get(creditorId).compareTo(new BigDecimal("0.01")) < 0) {
                    cIdx++;
                }
            }

            return ResponseEntity.ok(balancesList);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of("message", "Error calculating balances: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{groupId}/expenses/{expenseId}")
    @Transactional
    public ResponseEntity<?> deleteExpense(@PathVariable Long groupId, @PathVariable Long expenseId) {
        try {
            Expense expense = expenseRepository.findById(expenseId).orElse(null);
            if (expense == null) {
                return ResponseEntity.status(404).body(Map.of("message", "Expense not found"));
            }

            expenseRepository.delete(expense);
            return ResponseEntity.ok(Map.of("message", "Expense deleted successfully"));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of("message", "Failed to delete expense: " + e.getMessage()));
        }
    }
}