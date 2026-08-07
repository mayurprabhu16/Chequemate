package com.chequemate.controller;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
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

    // Helper method to eliminate boxed conversion diagnostics
    private Long parseUserId(Object obj) {
        if (obj == null) return null;
        if (obj instanceof Number num) {
            return num.longValue();
        }
        try {
            return Long.valueOf(String.valueOf(obj));
        } catch (NumberFormatException e) {
            return null;
        }
    }

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

    @PostMapping
    public ResponseEntity<?> createGroupFromPayload(@RequestBody Map<String, Object> payload) {
        try {
            Long userId = null;

            if (payload.get("userId") != null) {
                userId = parseUserId(payload.get("userId"));
            } else if (payload.get("createdById") != null) {
                userId = parseUserId(payload.get("createdById"));
            } else if (payload.get("createdBy") instanceof Map<?, ?> map && map.get("id") != null) {
                userId = parseUserId(map.get("id"));
            } else if (payload.get("user") instanceof Map<?, ?> map && map.get("id") != null) {
                userId = parseUserId(map.get("id"));
            }

            if (userId == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("User ID is required to create a group.");
            }

            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("User not found with id: " + userId);
            }
            User creator = userOpt.get();

            Group group = new Group();
            group.setName(payload.get("name") != null ? String.valueOf(payload.get("name")) : "New Group");
            group.setMode(payload.get("mode") != null ? String.valueOf(payload.get("mode")) : "EQUAL");
            group.setCreatedBy(creator);

            Set<User> members = new HashSet<>();
            members.add(creator);
            group.setMembers(members);

            Group savedGroup = groupRepository.save(group);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedGroup);
        } catch (IllegalArgumentException | NoSuchElementException | ClassCastException e) {
            System.err.println("Invalid group payload: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid request payload: " + e.getMessage());
        } catch (RuntimeException e) {
            System.err.println("Error creating group: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error creating group: " + e.getMessage());
        }
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
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (RuntimeException e) {
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
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<?> deleteGroup(@PathVariable Long id) {
        try {
            Optional<Group> groupOpt = groupRepository.findById(id);
            if (groupOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Group not found with id: " + id);
            }

            List<Expense> expenses = expenseRepository.findByGroupId(id);
            if (expenses != null && !expenses.isEmpty()) {
                expenseRepository.deleteAll(expenses);
            }

            groupRepository.deleteById(id);
            return ResponseEntity.ok("Group deleted successfully");
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error deleting group: " + e.getMessage());
        }
    }

    @PostMapping("/{groupId}/members")
    public ResponseEntity<?> addMemberByPayload(@PathVariable Long groupId, @RequestBody Map<String, Object> payload) {
        try {
            Optional<Group> groupOpt = groupRepository.findById(groupId);
            if (groupOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Group not found with id: " + groupId);
            }
            Group group = groupOpt.get();

            Optional<User> userOpt = Optional.empty();

            String inputVal = null;
            if (payload.containsKey("userCode") && payload.get("userCode") != null) {
                inputVal = String.valueOf(payload.get("userCode")).trim();
            } else if (payload.containsKey("email") && payload.get("email") != null) {
                inputVal = String.valueOf(payload.get("email")).trim();
            } else if (payload.containsKey("userId") && payload.get("userId") != null) {
                inputVal = String.valueOf(payload.get("userId")).trim();
            }

            if (inputVal == null || inputVal.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Member identification input is required.");
            }

            // 1. Check for User Code format (e.g. "A00002" -> ID 2)
            if (inputVal.toUpperCase().startsWith("A") && inputVal.length() > 1) {
                try {
                    Long parsedId = parseUserId(inputVal.substring(1));
                    if (parsedId != null) userOpt = userRepository.findById(parsedId);
                } catch (NumberFormatException ignored) {}
            }

            // 2. Check for raw numeric User ID
            if (userOpt.isEmpty()) {
                Long parsedId = parseUserId(inputVal);
                if (parsedId != null) userOpt = userRepository.findById(parsedId);
            }

            // 3. Search by Email
            if (userOpt.isEmpty()) {
                userOpt = userRepository.findByEmail(inputVal);
            }

            // 4. Fallback search across all users for custom userCode field matching
            if (userOpt.isEmpty()) {
                List<User> allUsers = userRepository.findAll();
                for (User u : allUsers) {
                    if (u.getUserCode() != null && u.getUserCode().equalsIgnoreCase(inputVal)) {
                        userOpt = Optional.of(u);
                        break;
                    }
                }
            }

            if (userOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found with code/email: " + inputVal);
            }

            User user = userOpt.get();
            if (group.getMembers() == null) {
                group.setMembers(new HashSet<>());
            }

            group.getMembers().add(user);
            Group updatedGroup = groupRepository.save(group);

            return ResponseEntity.ok(updatedGroup);
        } catch (IllegalArgumentException | NoSuchElementException | ClassCastException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid data format: " + e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error adding member: " + e.getMessage());
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

            String description = payload.get("description") != null ? String.valueOf(payload.get("description")) : "Expense";
            expense.setDescription(description);

            BigDecimal amountVal = BigDecimal.ZERO;
            if (payload.get("amount") != null) {
                amountVal = new BigDecimal(String.valueOf(payload.get("amount")));
            } else if (payload.get("totalAmount") != null) {
                amountVal = new BigDecimal(String.valueOf(payload.get("totalAmount")));
            }
            expense.setAmount(amountVal.doubleValue());
            expense.setTotalAmount(amountVal);

            String splitType = payload.get("splitType") != null ? String.valueOf(payload.get("splitType")) : "EQUAL";
            expense.setSplitType(splitType);

            Long paidByUserId = null;
            Object paidByObj = payload.get("paidBy");
            if (paidByObj == null) {
                paidByObj = payload.get("paidByUserId");
            }

            if (paidByObj instanceof Map<?, ?> map) {
                Object idVal = map.get("id");
                if (idVal != null) paidByUserId = parseUserId(idVal);
            } else if (paidByObj != null) {
                paidByUserId = parseUserId(paidByObj);
            }

            if (paidByUserId != null) {
                userRepository.findById(paidByUserId).ifPresent(expense::setPaidBy);
            }

            List<ExpenseSplit> expenseSplits = new ArrayList<>();
            Object splitsObj = payload.get("splits");
            if (splitsObj == null) splitsObj = payload.get("memberSplits");

            if (splitsObj instanceof List<?> splitsList && !splitsList.isEmpty()) {
                for (Object splitObj : splitsList) {
                    if (splitObj instanceof Map<?, ?> splitMap) {
                        ExpenseSplit split = new ExpenseSplit();
                        split.setExpense(expense);

                        Long splitUserId = null;
                        if (splitMap.get("userId") != null) {
                            splitUserId = parseUserId(splitMap.get("userId"));
                        } else if (splitMap.get("user") != null) {
                            Object uObj = splitMap.get("user");
                            if (uObj instanceof Map<?, ?> uMap && uMap.get("id") != null) {
                                splitUserId = parseUserId(uMap.get("id"));
                            } else if (uObj != null) {
                                splitUserId = parseUserId(uObj);
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
            } else {
                Set<User> members = group.getMembers();
                if (members != null && !members.isEmpty() && amountVal.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal splitPerMember = amountVal.divide(BigDecimal.valueOf(members.size()), 2, RoundingMode.HALF_UP);
                    for (User member : members) {
                        ExpenseSplit split = new ExpenseSplit();
                        split.setExpense(expense);
                        split.setUser(member);
                        split.setAmount(splitPerMember);
                        split.setAmountOwed(splitPerMember);
                        expenseSplits.add(split);
                    }
                }
            }

            expense.setSplits(expenseSplits);

            Expense savedExpense = expenseRepository.save(expense);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedExpense);

        } catch (IllegalArgumentException | ClassCastException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid request data: " + e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing expense: " + e.getMessage());
        }
    }

    @DeleteMapping("/{groupId}/expenses/{expenseId}")
    public ResponseEntity<?> deleteGroupExpense(@PathVariable Long groupId, @PathVariable Long expenseId) {
        try {
            if (!expenseRepository.existsById(expenseId)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Expense not found with id: " + expenseId);
            }
            expenseRepository.deleteById(expenseId);
            return ResponseEntity.ok("Expense deleted successfully");
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error deleting expense: " + e.getMessage());
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

            Map<Long, BigDecimal> netBalances = new HashMap<>();
            Map<Long, User> userLookup = new HashMap<>();

            Set<User> members = group.getMembers() != null ? group.getMembers() : new HashSet<>();
            for (User member : members) {
                if (member != null && member.getId() != null) {
                    netBalances.put(member.getId(), BigDecimal.ZERO);
                    userLookup.put(member.getId(), member);
                }
            }

            for (Expense expense : expenses) {
                if (expense == null) continue;

                BigDecimal total = expense.getTotalAmount() != null ? 
                        expense.getTotalAmount() : 
                        (expense.getAmount() != null ? BigDecimal.valueOf(expense.getAmount()) : BigDecimal.ZERO);

                if (total.compareTo(BigDecimal.ZERO) <= 0) continue;

                User payer = expense.getPaidBy();
                if (payer != null && payer.getId() != null) {
                    Long payerId = payer.getId();
                    userLookup.putIfAbsent(payerId, payer);
                    netBalances.put(payerId, netBalances.getOrDefault(payerId, BigDecimal.ZERO).add(total));
                }

                List<ExpenseSplit> splits = expense.getSplits();
                if (splits != null && !splits.isEmpty()) {
                    for (ExpenseSplit split : splits) {
                        if (split != null && split.getUser() != null && split.getUser().getId() != null) {
                            Long debtorId = split.getUser().getId();
                            userLookup.putIfAbsent(debtorId, split.getUser());
                            BigDecimal owed = split.getAmountOwed() != null ? split.getAmountOwed() : 
                                    (split.getAmount() != null ? split.getAmount() : BigDecimal.ZERO);
                            netBalances.put(debtorId, netBalances.getOrDefault(debtorId, BigDecimal.ZERO).subtract(owed));
                        }
                    }
                } else if (!members.isEmpty()) {
                    BigDecimal splitPerMember = total.divide(BigDecimal.valueOf(members.size()), 2, RoundingMode.HALF_UP);
                    for (User member : members) {
                        if (member != null && member.getId() != null) {
                            netBalances.put(member.getId(), netBalances.getOrDefault(member.getId(), BigDecimal.ZERO).subtract(splitPerMember));
                        }
                    }
                }
            }

            PriorityQueue<Map.Entry<Long, BigDecimal>> debtors = new PriorityQueue<>(Comparator.comparing(Map.Entry::getValue));
            PriorityQueue<Map.Entry<Long, BigDecimal>> creditors = new PriorityQueue<>((a, b) -> b.getValue().compareTo(a.getValue()));

            for (Map.Entry<Long, BigDecimal> entry : netBalances.entrySet()) {
                if (entry.getValue().compareTo(BigDecimal.valueOf(-0.01)) < 0) {
                    debtors.add(new AbstractMap.SimpleEntry<>(entry.getKey(), entry.getValue()));
                } else if (entry.getValue().compareTo(BigDecimal.valueOf(0.01)) > 0) {
                    creditors.add(new AbstractMap.SimpleEntry<>(entry.getKey(), entry.getValue()));
                }
            }

            List<Map<String, Object>> simplifiedDebts = new ArrayList<>();

            while (!debtors.isEmpty() && !creditors.isEmpty()) {
                Map.Entry<Long, BigDecimal> debtor = debtors.poll();
                Map.Entry<Long, BigDecimal> creditor = creditors.poll();

                BigDecimal debtorOwes = debtor.getValue().negate();
                BigDecimal creditorGets = creditor.getValue();

                BigDecimal settledAmount = debtorOwes.min(creditorGets);

                User debtorUser = userLookup.get(debtor.getKey());
                User creditorUser = userLookup.get(creditor.getKey());

                Map<String, Object> debt = new HashMap<>();
                debt.put("fromUserId", debtor.getKey());
                debt.put("fromUserName", debtorUser != null ? debtorUser.getName() : "User " + debtor.getKey());
                debt.put("fromUserEmail", debtorUser != null ? debtorUser.getEmail() : "");
                debt.put("toUserId", creditor.getKey());
                debt.put("toUserName", creditorUser != null ? creditorUser.getName() : "User " + creditor.getKey());
                debt.put("toUserEmail", creditorUser != null ? creditorUser.getEmail() : "");
                debt.put("amount", settledAmount.setScale(2, RoundingMode.HALF_UP));

                simplifiedDebts.add(debt);

                if (debtorOwes.compareTo(settledAmount) > 0) {
                    debtors.add(new AbstractMap.SimpleEntry<>(debtor.getKey(), settledAmount.subtract(debtorOwes)));
                }

                if (creditorGets.compareTo(settledAmount) > 0) {
                    creditors.add(new AbstractMap.SimpleEntry<>(creditor.getKey(), creditorGets.subtract(settledAmount)));
                }
            }

            return ResponseEntity.ok(simplifiedDebts);

        } catch (IllegalArgumentException | NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }
}