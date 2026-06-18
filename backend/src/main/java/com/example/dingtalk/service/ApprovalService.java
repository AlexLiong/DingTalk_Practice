package com.example.dingtalk.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.dingtalk.common.BizException;
import com.example.dingtalk.entity.SysApproval;
import com.example.dingtalk.entity.SysUser;
import com.example.dingtalk.mapper.ApprovalMapper;
import com.example.dingtalk.mapper.UserMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ApprovalService {

    private final ApprovalMapper approvalMapper;
    private final UserMapper userMapper;

    public ApprovalService(ApprovalMapper approvalMapper, UserMapper userMapper) {
        this.approvalMapper = approvalMapper;
        this.userMapper = userMapper;
    }

    /** 我发起的 */
    public List<SysApproval> myApplied(Long userId) {
        List<SysApproval> list = approvalMapper.selectList(
                new LambdaQueryWrapper<SysApproval>()
                        .eq(SysApproval::getApplicantId, userId)
                        .orderByDesc(SysApproval::getCreateTime));
        list.forEach(this::fillNames);
        return list;
    }

    /** 待我审批的 */
    public List<SysApproval> pendingForMe(Long userId) {
        List<SysApproval> list = approvalMapper.selectList(
                new LambdaQueryWrapper<SysApproval>()
                        .eq(SysApproval::getApproverId, userId)
                        .eq(SysApproval::getStatus, 0)
                        .orderByDesc(SysApproval::getCreateTime));
        list.forEach(this::fillNames);
        return list;
    }

    /** 所有 (我发起 + 待我审批 + 我已审批) */
    public List<SysApproval> myAll(Long userId) {
        List<SysApproval> list = approvalMapper.selectList(
                new LambdaQueryWrapper<SysApproval>()
                        .and(q -> q.eq(SysApproval::getApplicantId, userId)
                                .or().eq(SysApproval::getApproverId, userId))
                        .orderByDesc(SysApproval::getCreateTime));
        list.forEach(this::fillNames);
        return list;
    }

    public void apply(SysApproval approval, Long applicantId) {
        approval.setApplicantId(applicantId);
        approval.setStatus(0);
        approval.setCreateTime(LocalDateTime.now());
        approvalMapper.insert(approval);
    }

    public void approve(Long id, Long approverId, int status, String remark) {
        SysApproval a = approvalMapper.selectById(id);
        if (a == null) throw new BizException("审批不存在");
        if (!a.getApproverId().equals(approverId)) throw new BizException("你不是审批人");
        if (a.getStatus() != 0) throw new BizException("该审批已处理");
        a.setStatus(status);
        a.setRemark(remark);
        a.setApproveTime(LocalDateTime.now());
        approvalMapper.updateById(a);
    }

    /** 统计: 待审批数 */
    public long pendingCount(Long userId) {
        return approvalMapper.selectCount(new LambdaQueryWrapper<SysApproval>()
                .eq(SysApproval::getApproverId, userId)
                .eq(SysApproval::getStatus, 0));
    }

    private void fillNames(SysApproval a) {
        SysUser applicant = userMapper.selectById(a.getApplicantId());
        if (applicant != null) a.setApplicantName(applicant.getNickname());
        if (a.getApproverId() != null) {
            SysUser approver = userMapper.selectById(a.getApproverId());
            if (approver != null) a.setApproverName(approver.getNickname());
        }
    }
}
