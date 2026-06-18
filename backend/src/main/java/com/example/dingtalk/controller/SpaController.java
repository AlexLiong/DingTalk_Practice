package com.example.dingtalk.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** SPA 前端路由 fallback: 把前端路由转发到 index.html (由 static 提供) */
@Controller
public class SpaController {

    @GetMapping(value = {"/", "/login", "/chat", "/documents", "/mailbox", "/ding", "/profile", "/notice", "/todo", "/approval", "/calendar", "/favorites", "/admin", "/admin/**"})
    public String index() {
        return "forward:/index.html";
    }
}
