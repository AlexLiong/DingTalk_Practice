package com.example.dingtalk.config;

import com.example.dingtalk.entity.OnlineUserStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Configuration
public class GlobalCacheConfig {
    /**
     * 在线用户状态
     */
    @Bean("onlineUserMap")
    public Map<Long, OnlineUserStatus> onlineUserMap() {
        return new ConcurrentHashMap<>(32);
    }
}
