package com.example.dingtalk.controller;

import com.example.dingtalk.common.Result;
import com.example.dingtalk.common.SecurityUtils;
import com.example.dingtalk.dto.ShareFileDTO;
import com.example.dingtalk.dto.UpdateFileDTO;
import com.example.dingtalk.entity.SysFile;
import com.example.dingtalk.service.FileService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/file")
public class FileController {

    private final FileService fileService;

    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    @PostMapping("/upload")
    public Result<SysFile> upload(@RequestParam("file") MultipartFile file) {
        return Result.ok(fileService.upload(file, SecurityUtils.getUserId()));
    }

    @GetMapping("/list")
    public Result<List<SysFile>> list(@RequestParam(defaultValue = "8") Integer limit) {
        return Result.ok(fileService.list(SecurityUtils.getUserId(), limit));
    }

    @PutMapping("/{id}")
    public Result<SysFile> update(@PathVariable Long id, @RequestBody UpdateFileDTO dto) {
        return Result.ok(fileService.update(id, SecurityUtils.getUserId(), dto));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        fileService.delete(id, SecurityUtils.getUserId());
        return Result.ok();
    }

    @PostMapping("/{id}/share")
    public Result<SysFile> share(@PathVariable Long id, @RequestBody(required = false) ShareFileDTO dto) {
        return Result.ok(fileService.share(id, SecurityUtils.getUserId(), dto == null ? null : dto.getTargetName()));
    }

    @PostMapping("/{id}/lock")
    public Result<SysFile> lock(@PathVariable Long id) {
        return Result.ok(fileService.lock(id, SecurityUtils.getUserId()));
    }

    @PostMapping("/{id}/recycle")
    public Result<SysFile> recycle(@PathVariable Long id) {
        return Result.ok(fileService.recycle(id, SecurityUtils.getUserId()));
    }
}
