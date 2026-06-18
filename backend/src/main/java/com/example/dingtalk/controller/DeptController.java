package com.example.dingtalk.controller;

import com.example.dingtalk.common.Result;
import com.example.dingtalk.entity.SysDept;
import com.example.dingtalk.service.DeptService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dept")
public class DeptController {

    private final DeptService deptService;

    public DeptController(DeptService deptService) {
        this.deptService = deptService;
    }

    @GetMapping("/tree")
    public Result<List<SysDept>> tree() {
        return Result.ok(deptService.tree());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('system:dept:add')")
    public Result<?> add(@RequestBody SysDept dept) {
        deptService.save(dept);
        return Result.ok();
    }

    @PutMapping
    @PreAuthorize("hasAuthority('system:dept:edit')")
    public Result<?> update(@RequestBody SysDept dept) {
        deptService.update(dept);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('system:dept:remove')")
    public Result<?> delete(@PathVariable Long id) {
        deptService.delete(id);
        return Result.ok();
    }
}
