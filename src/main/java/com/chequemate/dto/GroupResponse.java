package com.chequemate.dto;

import java.util.Set;

import com.chequemate.entity.Group;
import com.chequemate.entity.User;

public class GroupResponse {

    private Long id;
    private String name;
    private String description;
    private String mode;
    private Long createdById;
    private String createdByName;
    private String createdByEmail;
    private Set<User> members;

    public GroupResponse() {}

    public GroupResponse(Group group) {
        if (group != null) {
            this.id = group.getId();
            this.name = group.getName();
            this.description = group.getDescription();
            
            // Safe access for getMode()
            this.mode = group.getMode() != null ? group.getMode() : "EQUAL";

            // Safe access for getCreatedBy()
            User creator = group.getCreatedBy();
            if (creator != null) {
                this.createdById = creator.getId();
                this.createdByName = creator.getName();
                this.createdByEmail = creator.getEmail();
            }

            this.members = group.getMembers();
        }
    }

    // --- GETTERS & SETTERS ---

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public Long getCreatedById() {
        return createdById;
    }

    public void setCreatedById(Long createdById) {
        this.createdById = createdById;
    }

    public String getCreatedByName() {
        return createdByName;
    }

    public void setCreatedByName(String createdByName) {
        this.createdByName = createdByName;
    }

    public String getCreatedByEmail() {
        return createdByEmail;
    }

    public void setCreatedByEmail(String createdByEmail) {
        this.createdByEmail = createdByEmail;
    }

    public Set<User> getMembers() {
        return members;
    }

    public void setMembers(Set<User> members) {
        this.members = members;
    }
}