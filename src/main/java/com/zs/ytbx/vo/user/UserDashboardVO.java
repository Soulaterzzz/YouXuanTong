package com.zs.ytbx.vo.user;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class UserDashboardVO {

    private Profile profile;

    private List<MetricCard> metrics;

    private List<PolicyUpdate> recentPolicyUpdates;

    private AdvisorSnapshot advisorInfo;

    private List<QuickService> quickServices;

    @Data
    @Builder
    public static class Profile {
        private String customerName;
        private String welcomeText;
        private String memberLevel;
    }

    @Data
    @Builder
    public static class MetricCard {
        private String metricCode;
        private String metricName;
        private String metricValue;
        private String metricTag;
    }

    @Data
    @Builder
    public static class PolicyUpdate {
        private String productName;
        private String updateDate;
        private String content;
        private List<String> tags;
    }

    @Data
    @Builder
    public static class AdvisorSnapshot {
        private Long advisorId;
        private String advisorName;
        private String employeeNo;
        private String summary;
    }

    @Data
    @Builder
    public static class QuickService {
        private String serviceCode;
        private String serviceName;
        private String servicePath;
    }
}
