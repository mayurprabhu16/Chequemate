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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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
    @Transactional
    public ResponseEntity<?> createGroup(@PathVariable Long userId, @RequestBody Group group) {
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new NoSuchElementException("User not found with id: " + userId));

            group.setCreatedBy(user);
            group.setMode(group.getMode() != null ? group.getMode() : "EQUAL");

            Set<User> members = new HashSet<>();
            members.add(user);
            group.setMembers(members);

            Group savedGroup = groupRepository.save(group);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedGroup);
        } catch (IllegalArgumentException | NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @PostMapping("/{groupId}/members/{userId}")
    @Transactional
    public ResponseEntity<?> addMemberToGroup(@PathVariable Long groupId, @PathVariable Long userId) {
        try {
            Group group = groupRepository.findById(groupId)
                    .orElseThrow(() -> new NoSuchElementException("Group not found with id: " + groupId));

            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new NoSuchElementException("User not found with id: " + userId));

            if (group.getMembers() == null) {
                group.setMembers(new HashSet<>());
            }

            group.getMembers().add(user);
            Group updatedGroup = groupRepository.save(group);

            return ResponseEntity.ok(updatedGroup);
        } catch (IllegalArgumentException | NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @GetMapping("/{groupId}/expenses")
    public ResponseEntity<List<Expense>> getGroupExpenses(@PathVariable Long groupId) {
        return ResponseEntity.ok(expenseRepository.findByGroupIdOrderByCreatedAtDesc(groupId));
    }

    @PostMapping("/{groupId}/expenses")
    @Transactional
    public ResponseEntity<?> createGroupExpense(@PathVariable Long groupId, @RequestBody Map<String, Object> payload) {
        try {
            Group group = groupRepository.findById(groupId)
                    .orElseThrow(() -> new NoSuchElementException("Group not found with id: " + groupId));

            Expense expense = new Expense();
            expense.setGroup(group);
            expense.setCreatedAt(LocalDateTime.now());

            if (payload.containsKey("description") && payload.get("description") != null) {
                expense.setDescription(String.valueOf(payload.get("description")));
            } else {
                expense.setDescription("Expense");
            }

            if (payload.containsKey("amount") && payload.get("amount") != null) {
                String amtStr = String.valueOf(payload.get("amount"));
                expense.setAmount(Double.valueOf(amtStr));
                expense.setTotalAmount(new BigDecimal(amtStr));
            } else {
                expense.setAmount(0.0);
                expense.setTotalAmount(BigDecimal.ZERO);
            }

            if (payload.containsKey("splitType") && payload.get("splitType") != null) {
                expense.setSplitType(String.valueOf(payload.get("splitType")));
            } else {
                expense.setSplitType("EQUAL");
            }

            if (payload.containsKey("paidBy") && payload.get("paidBy") != null) {
                Long paidByUserId = null;
                Object paidByObj = payload.get("paidBy");

                if (paidByObj instanceof Map) {
                    Object idVal = ((Map<?, ?>) paidByObj).get("id");
                    if (idVal != null) {
                        paidByUserId = Long.valueOf(String.valueOf(idVal));
                    }
                } else {
                    paidByUserId = Long.valueOf(String.valueOf(paidByObj));
                }

                if (paidByUserId != null) {
                    User payer = userRepository.findById(paidByUserId).orElse(null);
                    expense.setPaidBy(payer);
                }
            }

            List<ExpenseSplit> expenseSplits = new ArrayList<>();
            if (payload.containsKey("splits") && payload.get("splits") instanceof List) {
                List<?> splitsList = (List<?>) payload.get("splits");

                for (Object splitObj : splitsList) {
                    if (splitObj instanceof Map) {
                        Map<?, ?> splitMap = (Map<?, ?>) splitObj;
                        ExpenseSplit split = new ExpenseSplit();
                        split.setExpense(expense);

                        Long splitUserId = null;
                        if (splitMap.containsKey("userId") && splitMap.get("userId") != null) {
                            splitUserId = Long.valueOf(String.valueOf(splitMap.get("userId")));
                        } else if (splitMap.containsKey("user") && splitMap.get("user") != null) {
                            Object uObj = splitMap.get("user");
                            if (uObj instanceof Map) {
                                Object uId = ((Map<?, ?>) uObj).get("id");
                                if (uId != null) splitUserId = Long.valueOf(String.valueOf(uId));
                            } else {
                                splitUserId = Long.valueOf(String.valueOf(uObj));
                            }
                        }

                        if (splitUserId != null) {
                            userRepository.findById(splitUserId).ifPresent(split::setUser);
                        }

                        if (splitMap.containsKey("amount") && splitMap.get("amount") != null) {
                            BigDecimal splitAmt = new BigDecimal(String.valueOf(splitMap.get("amount")));
                            split.setAmount(splitAmt);
                            split.setAmountOwed(splitAmt);
                        } else {
                            split.setAmount(BigDecimal.ZERO);
                            split.setAmountOwed(BigDecimal.ZERO);
                        }

                        expenseSplits.add(split);
                    }
                }
            }
            expense.setSplits(expenseSplits);

            Expense savedExpense = expenseRepository.save(expense);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedExpense);

        } catch (IllegalArgumentException | NoSuchElementException | ClassCastException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error adding expense: " + e.getMessage());
        }
    }

    @GetMapping("/{groupId}/balances")
    public ResponseEntity<?> getGroupBalances(@PathVariable Long groupId) {
        try {
            Group group = groupRepository.findById(groupId)
                    .orElseThrow(() -> new NoSuchElementException("Group not found with id: " + groupId));

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
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @Override
    public String toString() {
        return "GroupController{}";
    }
}