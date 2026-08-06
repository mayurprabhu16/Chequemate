package com.chequemate.controller;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.chequemate.entity.Expense;
import com.chequemate.entity.ExpenseSplit;
import com.chequemate.entity.Group;
import com.chequemate.entity.User;
import com.chequemate.repository.ExpenseRepository;
import com.chequemate.repository.GroupRepository;
import com.chequemate.repository.UserRepository;

@RestController
@RequestMapping("/api/groups")
@CrossOrigin(origins = "*")
public class GroupController {

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ExpenseRepository expenseRepository;

    @GetMapping
    public ResponseEntity<List<Group>> getAllGroups() {
        return ResponseEntity.ok(groupRepository.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Group> getGroupById(@PathVariable Long id) {
        return groupRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Group>> getGroupsByUserId(@PathVariable Long userId) {
        return ResponseEntity.ok(groupRepository.findByUserId(userId));
    }

    @PostMapping("/user/{userId}")
    public ResponseEntity<?> createGroup(@PathVariable Long userId, @RequestBody Group group) {
        try {
            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("User not found with id: " + userId);
            }
            User user = userOpt.get();

            group.setCreatedBy(user);
            group.setMode(group.getMode() != null ? group.getMode() : "EQUAL");

            Set<User> members = new HashSet<>();
            members.add(user);
            group.setMembers(members);

            Group savedGroup = groupRepository.save(group);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedGroup);
        } catch (IllegalArgumentException | NoSuchElementException e) {
            System.err.println("Invalid request during group creation: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (RuntimeException e) {
            System.err.println("Error creating group: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateGroup(@PathVariable Long id, @RequestBody Map<String, Object> payload) {
        try {
            Optional<Group> groupOpt = groupRepository.findById(id);
            if (groupOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Group not found with id: " + id);
            }
            Group group = groupOpt.get();

            if (payload.containsKey("name") && payload.get("name") != null) {
                group.setName(String.valueOf(payload.get("name")));
            }
            if (payload.containsKey("mode") && payload.get("mode") != null) {
                group.setMode(String.valueOf(payload.get("mode")));
            }

            Group updatedGroup = groupRepository.save(group);
            return ResponseEntity.ok(updatedGroup);
        } catch (IllegalArgumentException | NoSuchElementException e) {
            System.err.println("Invalid payload for updateGroup: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (RuntimeException e) {
            System.err.println("Error updating group " + id + ": " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @PostMapping("/{groupId}/members/{userId}")
    public ResponseEntity<?> addMemberToGroup(@PathVariable Long groupId, @PathVariable Long userId) {
        try {
            Optional<Group> groupOpt = groupRepository.findById(groupId);
            if (groupOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Group not found with id: " + groupId);
            }
            Group group = groupOpt.get();

            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("User not found with id: " + userId);
            }
            User user = userOpt.get();

            if (group.getMembers() == null) {
                group.setMembers(new HashSet<>());
            }

            group.getMembers().add(user);
            Group updatedGroup = groupRepository.save(group);

            return ResponseEntity.ok(updatedGroup);
        } catch (IllegalArgumentException | NoSuchElementException e) {
            System.err.println("Invalid user or group ID: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (RuntimeException e) {
            System.err.println("Error adding member to group: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @GetMapping("/{groupId}/expenses")
    public ResponseEntity<List<Expense>> getGroupExpenses(@PathVariable Long groupId) {
        return ResponseEntity.ok(expenseRepository.findByGroupIdOrderByCreatedAtDesc(groupId));
    }

    @PostMapping("/{groupId}/expenses")
    public ResponseEntity<?> createGroupExpense(@PathVariable Long groupId, @RequestBody Map<String, Object> payload) {
        try {
            Optional<Group> groupOpt = groupRepository.findById(groupId);
            if (groupOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Group not found with id: " + groupId);
            }
            Group group = groupOpt.get();

            Expense expense = new Expense();
            expense.setGroup(group);
            expense.setCreatedAt(LocalDateTime.now());

            // 1. Parse Description
            String description = payload.get("description") != null ? String.valueOf(payload.get("description")) : "Expense";
            expense.setDescription(description);

            // 2. Parse Amount
            BigDecimal amountVal = BigDecimal.ZERO;
            if (payload.get("amount") != null) {
                amountVal = new BigDecimal(String.valueOf(payload.get("amount")));
            } else if (payload.get("totalAmount") != null) {
                amountVal = new BigDecimal(String.valueOf(payload.get("totalAmount")));
            }
            expense.setAmount(amountVal.doubleValue());
            expense.setTotalAmount(amountVal);

            // 3. Parse Split Type
            String splitType = payload.get("splitType") != null ? String.valueOf(payload.get("splitType")) : "EQUAL";
            expense.setSplitType(splitType);

            // 4. Parse PaidBy User safely
            Long paidByUserId = null;
            Object paidByObj = payload.get("paidBy");
            if (paidByObj == null) {
                paidByObj = payload.get("paidByUserId");
            }

            if (paidByObj instanceof Map<?, ?> map) {
                Object idVal = map.get("id");
                if (idVal != null) paidByUserId = Long.valueOf(String.valueOf(idVal));
            } else if (paidByObj != null) {
                paidByUserId = Long.valueOf(String.valueOf(paidByObj));
            }

            if (paidByUserId != null) {
                userRepository.findById(paidByUserId).ifPresent(expense::setPaidBy);
            }

            // 5. Parse Splits / Member Splits safely
            List<ExpenseSplit> expenseSplits = new ArrayList<>();
            Object splitsObj = payload.get("splits");
            if (splitsObj == null) splitsObj = payload.get("memberSplits");

            if (splitsObj instanceof List<?> splitsList) {
                for (Object splitObj : splitsList) {
                    if (splitObj instanceof Map<?, ?> splitMap) {
                        ExpenseSplit split = new ExpenseSplit();
                        split.setExpense(expense);

                        Long splitUserId = null;
                        if (splitMap.get("userId") != null) {
                            splitUserId = Long.valueOf(String.valueOf(splitMap.get("userId")));
                        } else if (splitMap.get("user") != null) {
                            Object uObj = splitMap.get("user");
                            if (uObj instanceof Map<?, ?> uMap && uMap.get("id") != null) {
                                splitUserId = Long.valueOf(String.valueOf(uMap.get("id")));
                            } else if (uObj != null) {
                                splitUserId = Long.valueOf(String.valueOf(uObj));
                            }
                        }

                        if (splitUserId != null) {
                            userRepository.findById(splitUserId).ifPresent(split::setUser);
                        }

                        BigDecimal splitAmt = BigDecimal.ZERO;
                        if (splitMap.get("amount") != null) {
                            splitAmt = new BigDecimal(String.valueOf(splitMap.get("amount")));
                        } else if (splitMap.get("amountOwed") != null) {
                            splitAmt = new BigDecimal(String.valueOf(splitMap.get("amountOwed")));
                        }

                        split.setAmount(splitAmt);
                        split.setAmountOwed(splitAmt);

                        if (split.getUser() != null) {
                            expenseSplits.add(split);
                        }
                    }
                }
            }

            expense.setSplits(expenseSplits);

            // 6. Save to Database
            Expense savedExpense = expenseRepository.save(expense);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedExpense);

        } catch (IllegalArgumentException | ClassCastException e) {
            System.err.println("Invalid arguments processing expense for group " + groupId + ": " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid request data: " + e.getMessage());
        } catch (RuntimeException e) {
            System.err.println("Server error processing expense for group " + groupId + ": " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing expense: " + e.getMessage());
        }
    }

    @GetMapping("/{groupId}/balances")
    public ResponseEntity<?> getGroupBalances(@PathVariable Long groupId) {
        try {
            Optional<Group> groupOpt = groupRepository.findById(groupId);
            if (groupOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Group not found with id: " + groupId);
            }
            Group group = groupOpt.get();

            List<Expense> expenses = expenseRepository.findByGroupId(groupId);

            Map<Long, BigDecimal> balances = new HashMap<>();

            if (group.getMembers() != null) {
                for (User member : group.getMembers()) {
                    if (member != null && member.getId() != null) {
                        balances.put(member.getId(), BigDecimal.ZERO);
                    }
                }
            }

            for (Expense expense : expenses) {
                if (expense != null) {
                    Optional<User> optionalPayer = Optional.ofNullable(expense.getPaidBy());
                    if (optionalPayer.isPresent()) {
                        User payer = optionalPayer.get();
                        Long payerId = payer.getId();
                        if (payerId != null) {
                            BigDecimal total = expense.getTotalAmount() != null ? 
                                    expense.getTotalAmount() : 
                                    (expense.getAmount() != null ? BigDecimal.valueOf(expense.getAmount()) : BigDecimal.ZERO);

                            balances.put(payerId, balances.getOrDefault(payerId, BigDecimal.ZERO).add(total));
                        }
                    }

                    if (expense.getSplits() != null) {
                        for (ExpenseSplit split : expense.getSplits()) {
                            if (split != null && split.getUser() != null && split.getUser().getId() != null && split.getAmountOwed() != null) {
                                Long debtorId = split.getUser().getId();
                                balances.put(debtorId, balances.getOrDefault(debtorId, BigDecimal.ZERO).subtract(split.getAmountOwed()));
                            }
                        }
                    }
                }
            }

            return ResponseEntity.ok(balances);
        } catch (IllegalArgumentException | NoSuchElementException e) {
            System.err.println("Invalid request fetching balances for group " + groupId + ": " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (RuntimeException e) {
            System.err.println("Error calculating balances for group " + groupId + ": " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }
}