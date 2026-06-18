package com.example.dingtalk.controller;

import com.example.dingtalk.common.Result;
import com.example.dingtalk.common.SecurityUtils;
import com.example.dingtalk.dto.SendDingDTO;
import com.example.dingtalk.entity.SysDing;
import com.example.dingtalk.service.DingService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ding")
public class DingController {

    private final DingService dingService;

    public DingController(DingService dingService) {
        this.dingService = dingService;
    }

    @GetMapping("/list")
    public Result<List<SysDing>> list() {
        return Result.ok(dingService.list(SecurityUtils.getUserId()));
    }

    @PostMapping("/reset")
    public Result<?> reset() {
        dingService.resetSamples(SecurityUtils.getUserId());
        return Result.ok();
    }

    @PostMapping("/send")
    public Result<?> send(@RequestBody SendDingDTO dto) {
        dingService.send(SecurityUtils.getUserId(), dto);
        return Result.ok();
    }

    @PostMapping("/{id}/confirm")
    public Result<?> confirm(@PathVariable Long id) {
        dingService.confirm(SecurityUtils.getUserId(), id);
        return Result.ok();
    }

    @PostMapping("/{id}/done")
    public Result<?> done(@PathVariable Long id) {
        dingService.done(SecurityUtils.getUserId(), id);
        return Result.ok();
    }

    @PostMapping("/{id}/remind")
    public Result<?> remind(@PathVariable Long id) {
        dingService.remind(SecurityUtils.getUserId(), id);
        return Result.ok();
    }

    @PutMapping("/{id}/note")
    public Result<?> saveNote(@PathVariable Long id, @RequestBody Map<String, String> body) {
        dingService.saveNote(SecurityUtils.getUserId(), id, body.get("note"));
        return Result.ok();
    }
}
