package com.example.dingtalk.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.dingtalk.common.Result;
import com.example.dingtalk.common.SecurityUtils;
import com.example.dingtalk.entity.ChatFavorite;
import com.example.dingtalk.mapper.FavoriteMapper;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/favorite")
public class FavoriteController {

    private final FavoriteMapper favoriteMapper;

    public FavoriteController(FavoriteMapper favoriteMapper) {
        this.favoriteMapper = favoriteMapper;
    }

    @GetMapping("/list")
    public Result<List<ChatFavorite>> list() {
        return Result.ok(favoriteMapper.selectList(
                new LambdaQueryWrapper<ChatFavorite>()
                        .eq(ChatFavorite::getUserId, SecurityUtils.getUserId())
                        .orderByDesc(ChatFavorite::getCreateTime)));
    }

    @PostMapping
    public Result<?> add(@RequestBody ChatFavorite fav) {
        fav.setUserId(SecurityUtils.getUserId());
        fav.setCreateTime(LocalDateTime.now());
        // 防重复收藏
        Long cnt = favoriteMapper.selectCount(new LambdaQueryWrapper<ChatFavorite>()
                .eq(ChatFavorite::getUserId, fav.getUserId())
                .eq(ChatFavorite::getMessageId, fav.getMessageId()));
        if (cnt > 0) return Result.fail("已收藏过");
        favoriteMapper.insert(fav);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        favoriteMapper.deleteById(id);
        return Result.ok();
    }
}
