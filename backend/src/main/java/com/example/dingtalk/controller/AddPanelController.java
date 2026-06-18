package com.example.dingtalk.controller;

import com.example.dingtalk.common.Result;
import com.example.dingtalk.common.SecurityUtils;
import com.example.dingtalk.dto.RememberAddUsageDTO;
import com.example.dingtalk.dto.SaveQuickLinkDTO;
import com.example.dingtalk.entity.ChatSavedLink;
import com.example.dingtalk.service.AddPanelService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/add-panel")
public class AddPanelController {

    private final AddPanelService addPanelService;

    public AddPanelController(AddPanelService addPanelService) {
        this.addPanelService = addPanelService;
    }

    @GetMapping("/links")
    public Result<List<ChatSavedLink>> listLinks() {
        return Result.ok(addPanelService.listLinks(SecurityUtils.getUserId()));
    }

    @PostMapping("/links")
    public Result<ChatSavedLink> saveLink(@RequestBody SaveQuickLinkDTO dto) {
        return Result.ok(addPanelService.saveLink(SecurityUtils.getUserId(), dto));
    }

    @DeleteMapping("/links/{id}")
    public Result<?> deleteLink(@PathVariable Long id) {
        addPanelService.deleteLink(SecurityUtils.getUserId(), id);
        return Result.ok();
    }

    @GetMapping("/recent-usage")
    public Result<List<String>> listRecentUsage() {
        return Result.ok(addPanelService.listRecentUsageKeys(SecurityUtils.getUserId()));
    }

    @PostMapping("/recent-usage")
    public Result<?> rememberRecentUsage(@RequestBody RememberAddUsageDTO dto) {
        addPanelService.rememberUsage(SecurityUtils.getUserId(), dto);
        return Result.ok();
    }

    @DeleteMapping("/recent-usage")
    public Result<?> clearRecentUsage() {
        addPanelService.clearRecentUsage(SecurityUtils.getUserId());
        return Result.ok();
    }
}
