package com.chequemate.dto;

import java.util.List;
import java.util.stream.Collectors;

import com.chequemate.entity.Group;
import com.chequemate.entity.User;

public class GroupResponse {
    private Long id;
    private String name;
    private String mode;
    private Long createdByUserId;
    private String createdByName;
    private List<MemberDTO> members;

    public GroupResponse() {}

    public static class MemberDTO {
        private Long id;
        private String name;
        private String email;

        public MemberDTO() {}

        public MemberDTO(User user) {
            this.id = user.getId();
            this.name = user.getName();
            this.email = user.getEmail();
        }

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
    }

    public static GroupResponse fromEntity(Group group) {
        GroupResponse response = new GroupResponse();
        response.setId(group.getId());
        response.setName(group.getName());
        response.setMode(group.getMode());

        if (group.getCreatedBy() != null) {
            response.setCreatedByUserId(group.getCreatedBy().getId());
            response.setCreatedByName(group.getCreatedBy().getName());
        }

        if (group.getMembers() != null) {
            List<MemberDTO> memberDTOs = group.getMembers().stream()
                    .map(MemberDTO::new)
                    .collect(Collectors.toList());
            response.setMembers(memberDTOs);
        }

        return response;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }

    public Long getCreatedByUserId() { return createdByUserId; }
    public void setCreatedByUserId(Long createdByUserId) { this.createdByUserId = createdByUserId; }

    public String getCreatedByName() { return createdByName; }
    public void setCreatedByName(String createdByName) { this.createdByName = createdByName; }

    public List<MemberDTO> getMembers() { return members; }
    public void setMembers(List<MemberDTO> members) { this.members = members; }
}