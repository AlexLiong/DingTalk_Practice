package com.example.dingtalk.controller;

import com.example.dingtalk.common.Result;
import com.example.dingtalk.common.SecurityUtils;
import com.example.dingtalk.entity.SysTodo;
import com.example.dingtalk.service.TodoService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/todo")
public class TodoController {

    private final TodoService todoService;

    public TodoController(TodoService todoService) {
        this.todoService = todoService;
    }

    @GetMapping("/list")
    public Result<List<SysTodo>> list(@RequestParam(required = false) Integer status) {
        return Result.ok(todoService.myList(SecurityUtils.getUserId(), status));
    }

    @GetMapping("/stats")
    public Result<Map<String, Long>> stats() {
        long[] s = todoService.stats(SecurityUtils.getUserId());
        return Result.ok(Map.of("pending", s[0], "inProgress", s[1], "done", s[2]));
    }

    @PostMapping
    public Result<?> add(@RequestBody SysTodo todo) {
        todoService.save(todo, SecurityUtils.getUserId());
        return Result.ok();
    }

    @PutMapping
    public Result<?> update(@RequestBody SysTodo todo) {
        todoService.update(todo);
        return Result.ok();
    }

    @PutMapping("/{id}/status")
    public Result<?> updateStatus(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        todoService.updateStatus(id, body.get("status"));
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        todoService.delete(id);
        return Result.ok();
    }
}
