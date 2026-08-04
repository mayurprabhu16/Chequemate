package com.chequemate.controller;

import com.chequemate.entity.Expense;
import com.chequemate.entity.ExpenseSplit;
import com.chequemate.entity.Group;
import com.chequemate.entity.User;
import com.chequemate.repository.ExpenseRepository;
import com.chequemate.repository.GroupRepository;
import com.chequemate.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/groups")
@CrossOrigin(origins = "*")
public class GroupController {

    private static final Logger logger = LoggerFactory.getLogger(GroupController.class);

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ExpenseRepository expenseRepository;

    @GetMapping("/user/{userId}")
    @Transactional(readOnly = true)
    public ResponseEntity<List<Group>> getUserGroups(@PathVariable Long userId) {
        List<Group> groups = groupRepository.findGroupsByUserIdNative(userId);
        return ResponseEntity.ok(groups);
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getGroupById(@PathVariable Long id) {
        Group group = groupRepository.findById(id).orElse(null);
        if (group == null) {
            return ResponseEntity.status(404).body(Map.of("message", "Group not found with id: " + id));
        }
        return ResponseEntity.ok(group);
    }

    @PostMapping
    @Transactional
    public ResponseEntity<?> createGroup(@RequestBody Map<String, Object> payload) {
        try {
            String name = (String) payload.get("name");
            String mode = (String) payload.get("mode");

            Long creatorId = null;
            if (payload.get("createdByUserId") != null) {
                try {
                    creatorId = Long.valueOf(payload.get("createdByUserId").toString());
                } catch (NumberFormatException ignored) {}
            }

            if (creatorId == null) {
                return ResponseEntity.badRequest().body(Map.of("message", "createdByUserId is required"));
            }

            User creator = userRepository.findById(creatorId).orElse(null);

            if (creator == null) {
                List<User> allUsers = userRepository.findAll();
                if (!allUsers.isEmpty()) {
                    creator = allUsers.get(allUsers.size() - 1);
                }
            }

            if (creator == null) {
                return ResponseEntity.badRequest().body(Map.of("message", "User not found in database. Please log in again."));
            }

            Group group = new Group();
            group.setName(name);
            group.setMode(mode);
            group.setCreatedBy(creator);

            Group savedGroup = groupRepository.saveAndFlush(group);

            if (savedGroup.getMembers() == null) {
                savedGroup.setMembers(new HashSet<>());
            }
            savedGroup.getMembers().add(creator);

            Group finalGroup = groupRepository.saveAndFlush(savedGroup);

            return ResponseEntity.ok(finalGroup);
        } catch (Exception e) {
            logger.error("Failed to create group", e);
            return ResponseEntity.internalServerError().body(Map.of("message", "Error creating group: " + e.getMessage()));
        }
    }

    @PostMapping("/{groupId}/members")
    @Transactional
    public ResponseEntity<?> addMemberToGroup(@PathVariable Long groupId, @RequestBody Map<String, String> payload) {
        try {
            Group group = groupRepository.findById(groupId).orElse(null);
            if (group == null) {
                return ResponseEntity.status(404).body(Map.of("message", "Group not found with id: " + groupId));
            }

            String searchVal = payload.get("userCode");
            if (searchVal == null || searchVal.trim().isEmpty()) {
                searchVal = payload.get("email");
            }

            if (searchVal == null || searchVal.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("message", "User email or code is required"));
            }

            String queryVal = searchVal.trim();
            Optional<User> userOpt = userRepository.findByUserCode(queryVal);

            if (userOpt.isEmpty()) {
                userOpt = userRepository.findByEmail(queryVal.toLowerCase());
            }

            if (userOpt.isEmpty()) {
                try {
                    Long parsedId = Long.valueOf(queryVal.replaceAll("[^0-9]", ""));
                    userOpt = userRepository.findById(parsedId);
                } catch (Exception ignored) {}
            }

            if (userOpt.isEmpty()) {
                return ResponseEntity.status(404).body(Map.of("message", "User '" + queryVal + "' not found"));
            }

            User newMember = userOpt.get();

            if (group.getMembers() == null) {
                group.setMembers(new HashSet<>());
            }

            // Add new member to group
            group.getMembers().add(newMember);
            Group updatedGroup = groupRepository.saveAndFlush(group);

            // Dynamically update past equal-split expenses to include the new member
            List<Expense> expenses = expenseRepository.findByGroupIdOrderByCreatedAtDesc(groupId);
            int totalMemberCount = updatedGroup.getMembers().size();

            if (totalMemberCount > 0) {
                for (Expense expense : expenses) {
                    if ("EQUAL".equalsIgnoreCase(expense.getSplitType())) {
                        boolean hasMember = expense.getSplits().stream()
                                .anyMatch(s -> s.getUser().getId().equals(newMember.getId()));

                        if (!hasMember) {
                            BigDecimal newShare = expense.getTotalAmount()
                                    .divide(BigDecimal.valueOf(totalMemberCount), 2, RoundingMode.HALF_UP);

                            // Update existing splits
                            for (ExpenseSplit split : expense.getSplits()) {
                                split.setAmountOwed(newShare);
                            }

                            // Add split entry for new member
                            expense.getSplits().add(new ExpenseSplit(expense, newMember, newShare));
                            expenseRepository.save(expense);
                        }
                    }
                }
            }

            return ResponseEntity.ok(updatedGroup);
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid payload for adding member", e);
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            logger.error("Failed to add member to group", e);
            return ResponseEntity.internalServerError().body(Map.of("message", "Failed to add member: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<Group> updateGroup(@PathVariable Long id, @RequestBody Group updatedGroup) {
        Group group = groupRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Group not found with id: " + id));

        group.setName(updatedGroup.getName());
        group.setMode(updatedGroup.getMode());
        Group saved = groupRepository.save(group);
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<?> deleteGroup(@PathVariable Long id) {
        Group group = groupRepository.findById(id).orElse(null);
        if (group == null) {
            return ResponseEntity.status(404).body(Map.of("message", "Group not found"));
        }

        group.getMembers().clear();
        groupRepository.delete(group);

        return ResponseEntity.ok(Map.of("message", "Group deleted successfully"));
    }
}