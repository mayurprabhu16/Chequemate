package com.chequemate.controller;

import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @PostMapping("/{groupId}/members/{userId}")
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
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @GetMapping("/{groupId}/expenses")
    public ResponseEntity<List<Expense>> getGroupExpenses(@PathVariable Long groupId) {
        return ResponseEntity.ok(expenseRepository.findByGroupIdOrderByCreatedAtDesc(groupId));
    }

    @PostMapping("/{groupId}/expenses")
    public ResponseEntity<?> createGroupExpense(@PathVariable Long groupId, @RequestBody Expense expense) {
        try {
            Group group = groupRepository.findById(groupId)
                    .orElseThrow(() -> new NoSuchElementException("Group not found with id: " + groupId));

            expense.setGroup(group);

            if (expense.getSplits() != null && !expense.getSplits().isEmpty()) {
                for (ExpenseSplit split : expense.getSplits()) {
                    split.setExpense(expense);
                    if (split.getAmount() != null) {
                        split.setAmountOwed(split.getAmount());
                    }
                }
            }

            Expense savedExpense = expenseRepository.save(expense);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedExpense);
        } catch (IllegalArgumentException | NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }
}