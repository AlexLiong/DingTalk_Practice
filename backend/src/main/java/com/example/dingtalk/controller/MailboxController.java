package com.example.dingtalk.controller;

import com.example.dingtalk.common.Result;
import com.example.dingtalk.common.SecurityUtils;
import com.example.dingtalk.dto.SendMailDTO;
import com.example.dingtalk.entity.SysMailboxMail;
import com.example.dingtalk.service.MailboxService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/mailbox")
public class MailboxController {

    private final MailboxService mailboxService;

    public MailboxController(MailboxService mailboxService) {
        this.mailboxService = mailboxService;
    }

    @GetMapping("/list")
    public Result<List<SysMailboxMail>> list() {
        return Result.ok(mailboxService.list(SecurityUtils.getUserId()));
    }

    @PostMapping("/reset")
    public Result<?> reset() {
        mailboxService.resetSamples(SecurityUtils.getUserId());
        return Result.ok();
    }

    @PostMapping("/send")
    public Result<?> send(@RequestBody SendMailDTO dto) {
        mailboxService.send(SecurityUtils.getUserId(), dto);
        return Result.ok();
    }

    @PostMapping("/{id}/open")
    public Result<?> open(@PathVariable Long id) {
        mailboxService.open(SecurityUtils.getUserId(), id);
        return Result.ok();
    }

    @PutMapping("/{id}/read")
    public Result<?> updateRead(@PathVariable Long id, @RequestBody Map<String, Boolean> body) {
        mailboxService.updateRead(SecurityUtils.getUserId(), id, Boolean.TRUE.equals(body.get("unread")));
        return Result.ok();
    }

    @PutMapping("/{id}/star")
    public Result<?> updateStar(@PathVariable Long id, @RequestBody Map<String, Boolean> body) {
        mailboxService.updateStar(SecurityUtils.getUserId(), id, Boolean.TRUE.equals(body.get("starred")));
        return Result.ok();
    }

    @PutMapping("/{id}/archive")
    public Result<?> archive(@PathVariable Long id) {
        mailboxService.archive(SecurityUtils.getUserId(), id);
        return Result.ok();
    }

    @PutMapping("/{id}/draft")
    public Result<?> saveDraft(@PathVariable Long id, @RequestBody Map<String, String> body) {
        mailboxService.saveDraft(SecurityUtils.getUserId(), id, body.get("draft"));
        return Result.ok();
    }

    @PostMapping("/{id}/reply")
    public Result<?> reply(@PathVariable Long id, @RequestBody Map<String, String> body) {
        mailboxService.sendReply(SecurityUtils.getUserId(), id, body.get("content"));
        return Result.ok();
    }
}
