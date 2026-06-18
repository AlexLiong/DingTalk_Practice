package com.example.dingtalk.controller;

import com.example.dingtalk.common.Result;
import com.example.dingtalk.common.SecurityUtils;
import com.example.dingtalk.entity.SysApproval;
import com.example.dingtalk.service.ApprovalService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/approval")
public class ApprovalController {

    private final ApprovalService approvalService;

    public ApprovalController(ApprovalService approvalService) {
        this.approvalService = approvalService;
    }

    /** 我的所有审批 (发起+审批) */
    @GetMapping("/list")
    public Result<List<SysApproval>> list() {
        return Result.ok(approvalService.myAll(SecurityUtils.getUserId()));
    }

    /** 待我审批 */
    @GetMapping("/pending")
    public Result<List<SysApproval>> pending() {
        return Result.ok(approvalService.pendingForMe(SecurityUtils.getUserId()));
    }

    /** 我发起的 */
    @GetMapping("/applied")
    public Result<List<SysApproval>> applied() {
        return Result.ok(approvalService.myApplied(SecurityUtils.getUserId()));
    }

    /** 发起审批 */
    @PostMapping
    public Result<?> apply(@RequestBody SysApproval approval) {
        approvalService.apply(approval, SecurityUtils.getUserId());
        return Result.ok();
    }

    /** 审批操作 */
    @PutMapping("/{id}")
    public Result<?> approve(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        int status = (int) body.get("status");    // 1通过 2驳回
        String remark = (String) body.getOrDefault("remark", "");
        approvalService.approve(id, SecurityUtils.getUserId(), status, remark);
        return Result.ok();
    }
}
