package com.example.dingtalk.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.dingtalk.common.Result;
import com.example.dingtalk.common.SecurityUtils;
import com.example.dingtalk.entity.SysNotice;
import com.example.dingtalk.service.NoticeService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notice")
public class NoticeController {

    private final NoticeService noticeService;

    public NoticeController(NoticeService noticeService) {
        this.noticeService = noticeService;
    }

    /** 前台: 已发布公告列表 */
    @GetMapping("/list")
    public Result<IPage<SysNotice>> list(@RequestParam(defaultValue = "1") int pageNum,
                                         @RequestParam(defaultValue = "10") int pageSize) {
        return Result.ok(noticeService.page(pageNum, pageSize, SecurityUtils.getUserId()));
    }

    /** 前台: 公告详情(自动标记已读) */
    @GetMapping("/{id}")
    public Result<SysNotice> detail(@PathVariable Long id) {
        return Result.ok(noticeService.getById(id, SecurityUtils.getUserId()));
    }

    /** 管理: 全部公告 */
    @GetMapping("/admin/list")
    @PreAuthorize("hasAuthority('notice:list')")
    public Result<IPage<SysNotice>> adminList(@RequestParam(defaultValue = "1") int pageNum,
                                              @RequestParam(defaultValue = "10") int pageSize) {
        return Result.ok(noticeService.adminPage(pageNum, pageSize));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('notice:add')")
    public Result<?> add(@RequestBody SysNotice notice) {
        notice.setPublisherId(SecurityUtils.getUserId());
        noticeService.save(notice);
        return Result.ok();
    }

    @PutMapping
    @PreAuthorize("hasAuthority('notice:edit')")
    public Result<?> update(@RequestBody SysNotice notice) {
        noticeService.update(notice);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('notice:remove')")
    public Result<?> delete(@PathVariable Long id) {
        noticeService.delete(id);
        return Result.ok();
    }
}
