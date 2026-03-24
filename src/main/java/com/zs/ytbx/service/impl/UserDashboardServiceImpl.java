package com.zs.ytbx.service.impl;

import com.zs.ytbx.service.UserDashboardService;
import com.zs.ytbx.vo.user.UserDashboardVO;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@ConditionalOnProperty(name = "ytbx.mock.enabled", havingValue = "true", matchIfMissing = true)
public class UserDashboardServiceImpl implements UserDashboardService {

    @Override
    public UserDashboardVO getDashboard() {
        return UserDashboardVO.builder()
                .profile(UserDashboardVO.Profile.builder()
                        .customerName("张先森")
                        .welcomeText("您的数字信托账户运行良好。本月已有2份保单完成年度自动续保，资产保障额度提升了12.5%。")
                        .memberLevel("钻石尊享会员")
                        .build())
                .metrics(List.of(
                        UserDashboardVO.MetricCard.builder().metricCode("policy_count").metricName("已投保险数").metricValue("12份有效保单").metricTag("运行中").build(),
                        UserDashboardVO.MetricCard.builder().metricCode("monthly_cost").metricName("本月保障支出").metricValue("¥4,280.00").metricTag("本月").build(),
                        UserDashboardVO.MetricCard.builder().metricCode("claim_count").metricName("理赔进度数量").metricValue("02笔案件处理中").metricTag("理赔中").build()
                ))
                .recentPolicyUpdates(List.of(
                        UserDashboardVO.PolicyUpdate.builder().productName("平安e生保·长期医疗险").updateDate("10月24日").content("您的保单已成功续期，保障期限延长至2025-10-24。").tags(List.of("自动续费", "保障中")).build(),
                        UserDashboardVO.PolicyUpdate.builder().productName("机动车辆综合商业保险").updateDate("10月20日").content("理赔案件 [PA20241020] 已进入损失核定阶段，预计2个工作日内完成。").tags(List.of("理赔进度更新")).build()
                ))
                .advisorInfo(UserDashboardVO.AdvisorSnapshot.builder()
                        .advisorId(501L)
                        .advisorName("王若琳")
                        .employeeNo("PA-8823910")
                        .summary("根据您近期的家庭资产变化，建议关注《海外教育信托计划》。")
                        .build())
                .quickServices(List.of(
                        UserDashboardVO.QuickService.builder().serviceCode("claim_report").serviceName("报案理赔").servicePath("/claims/report").build(),
                        UserDashboardVO.QuickService.builder().serviceCode("e_policy").serviceName("电子保单").servicePath("/policy/list").build()
                ))
                .build();
    }
}
