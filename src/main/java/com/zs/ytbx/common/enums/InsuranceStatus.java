package com.zs.ytbx.common.enums;

import lombok.Getter;

import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;

@Getter
public enum InsuranceStatus {
    DRAFT("待提交", "投保资料已暂存，等待用户正式提交"),
    PENDING_REVIEW("待审核", "用户已提交投保资料，等待管理员审核"),
    APPROVED("审核通过", "管理员审核通过，等待进入承保流程"),
    REVIEW_REJECTED("审核驳回", "管理员审核未通过"),
    UNDERWRITING("承保中", "管理员已将订单推进至承保阶段"),
    ACTIVE("已生效", "保单已生效并生成正式保单号"),
    EXPIRED("已过期", "保单已过期"),
    CANCELLED("已取消", "保单已取消");

    private final String label;
    private final String description;

    InsuranceStatus(String label, String description) {
        this.label = label;
        this.description = description;
    }

    public String getCode() {
        return name();
    }

    public static InsuranceStatus fromCode(String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("保险状态不能为空");
        }

        String normalized = normalizeCode(code);
        for (InsuranceStatus status : values()) {
            if (status.name().equals(normalized)) {
                return status;
            }
        }
        throw new IllegalArgumentException("未知的保险状态：" + code);
    }

    public static String normalizeCode(String code) {
        String normalized = code.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "PENDING" -> UNDERWRITING.name();
            case "PROCESSING" -> UNDERWRITING.name();
            case "COMPLETED" -> ACTIVE.name();
            case "REJECTED" -> REVIEW_REJECTED.name();
            case "INSURED" -> ACTIVE.name();
            case "SUBMITTING" -> PENDING_REVIEW.name();
            default -> normalized;
        };
    }

    public boolean canTransitionTo(InsuranceStatus targetStatus) {
        if (targetStatus == null) {
            return false;
        }

        Set<InsuranceStatus> allowedTargets = switch (this) {
            case DRAFT -> EnumSet.of(PENDING_REVIEW, CANCELLED);
            case PENDING_REVIEW -> EnumSet.of(APPROVED, REVIEW_REJECTED, CANCELLED);
            case APPROVED -> EnumSet.of(UNDERWRITING, CANCELLED);
            case UNDERWRITING -> EnumSet.of(ACTIVE, CANCELLED);
            case ACTIVE -> EnumSet.of(EXPIRED, CANCELLED);
            case REVIEW_REJECTED, EXPIRED, CANCELLED -> EnumSet.noneOf(InsuranceStatus.class);
        };
        return allowedTargets.contains(targetStatus);
    }
}
