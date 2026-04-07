package com.zs.ytbx.service;

import com.zs.ytbx.common.api.PageResponse;
import com.zs.ytbx.query.PolicyQuery;
import com.zs.ytbx.vo.policy.PolicyDetailVO;
import com.zs.ytbx.vo.policy.PolicyDownloadVO;
import com.zs.ytbx.vo.policy.PolicyEventVO;
import com.zs.ytbx.vo.policy.PolicyListItemVO;

import java.util.List;

public interface PolicyService {

    PageResponse<PolicyListItemVO> listPolicies(PolicyQuery query);

    PolicyDetailVO getPolicyDetail(Long policyId);

    PolicyDownloadVO getEPolicy(Long policyId);

    List<PolicyEventVO> getEvents(Long policyId);
}
