package com.chequemate.dto;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ExpenseRequest {
    private String description;
    private BigDecimal amount;
    private BigDecimal totalAmount;
    private String splitType;
    private Long paidByUserId;
    private Long groupId;
    private List<SplitRequest> splits;
    private Object memberSplits;

    public ExpenseRequest() {}

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount != null ? totalAmount : amount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getSplitType() {
        return splitType;
    }

    public void setSplitType(String splitType) {
        this.splitType = splitType;
    }

    public Long getPaidByUserId() {
        return paidByUserId;
    }

    public void setPaidByUserId(Long paidByUserId) {
        this.paidByUserId = paidByUserId;
    }

    public Long getGroupId() {
        return groupId;
    }

    public void setGroupId(Long groupId) {
        this.groupId = groupId;
    }

    @SuppressWarnings("unchecked")
    public List<SplitRequest> getSplits() {
        if (splits != null) return splits;
        if (memberSplits instanceof List) {
            return (List<SplitRequest>) memberSplits;
        }
        return Collections.emptyList();
    }

    public void setSplits(List<SplitRequest> splits) {
        this.splits = splits;
    }

    public MemberSplitsWrapper getMemberSplits() {
        return new MemberSplitsWrapper(memberSplits != null ? memberSplits : splits);
    }

    public void setMemberSplits(Object memberSplits) {
        this.memberSplits = memberSplits;
    }

    public static class MemberSplitsWrapper {
        private final Object rawData;

        public MemberSplitsWrapper(Object rawData) {
            this.rawData = rawData;
        }

        public Object get(Object key) {
            if (rawData == null) return null;

            return switch (rawData) {
                case Map<?, ?> map -> {
                    if (map.containsKey(key)) yield map.get(key);
                    yield map.get(String.valueOf(key));
                }
                case List<?> list -> {
                    for (Object item : list) {
                        switch (item) {
                            case SplitRequest sr -> {
                                if (key instanceof Long l && l.equals(sr.getUserId())) {
                                    yield sr.getAmount();
                                }
                                if (key instanceof String s && String.valueOf(sr.getUserId()).equals(s)) {
                                    yield sr.getAmount();
                                }
                            }
                            case Map<?, ?> m -> {
                                Object uid = m.get("userId");
                                if (uid != null && String.valueOf(uid).equals(String.valueOf(key))) {
                                    yield m.get("amount");
                                }
                            }
                            case null, default -> {}
                        }
                    }
                    yield null;
                }
                default -> null;
            };
        }
    }

    public static class SplitRequest {
        private Long userId;
        private BigDecimal amount;
        private Double percentage;

        public SplitRequest() {}

        public Long getUserId() {
            return userId;
        }

        public void setUserId(Long userId) {
            this.userId = userId;
        }

        public BigDecimal getAmount() {
            return amount;
        }

        public void setAmount(BigDecimal amount) {
            this.amount = amount;
        }

        public Double getPercentage() {
            return percentage;
        }

        public void setPercentage(Double percentage) {
            this.percentage = percentage;
        }
    }
}