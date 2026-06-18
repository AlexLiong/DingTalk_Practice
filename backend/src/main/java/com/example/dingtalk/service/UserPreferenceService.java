package com.example.dingtalk.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.example.dingtalk.dto.UpdateUserPreferenceDTO;
import com.example.dingtalk.entity.SysUserPreference;
import com.example.dingtalk.mapper.UserPreferenceMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class UserPreferenceService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final TypeReference<LinkedHashMap<String, Object>> VIEW_STATE_TYPE = new TypeReference<>() { };

    private final UserPreferenceMapper userPreferenceMapper;

    public UserPreferenceService(UserPreferenceMapper userPreferenceMapper) {
        this.userPreferenceMapper = userPreferenceMapper;
    }

    @PostConstruct
    public void resetConnectedPresenceOnBoot() {
        LocalDateTime now = LocalDateTime.now();
        userPreferenceMapper.update(null, new LambdaUpdateWrapper<SysUserPreference>()
                .set(SysUserPreference::getConnectedOnline, 0)
                .set(SysUserPreference::getLastOfflineAt, now)
                .set(SysUserPreference::getUpdateTime, now)
                .eq(SysUserPreference::getConnectedOnline, 1));
    }

    @Transactional
    public Map<String, Object> getCurrent(Long userId) {
        SysUserPreference preference = ensurePreference(userId);
        touchActive(preference);
        return toPayload(preference);
    }

    @Transactional
    public Map<String, Object> updateCurrent(Long userId, UpdateUserPreferenceDTO dto) {
        SysUserPreference preference = ensurePreference(userId);
        LocalDateTime now = LocalDateTime.now();

        if (dto.getTheme() != null) {
            preference.setTheme(normalizeTheme(dto.getTheme()));
        }
        if (dto.getLastRoute() != null) {
            preference.setLastRoute(normalizeLastRoute(dto.getLastRoute()));
        }
        if (dto.getViewState() != null) {
            preference.setViewStateJson(writeViewState(dto.getViewState()));
        }

        preference.setLastActiveAt(now);
        preference.setUpdateTime(now);
        userPreferenceMapper.updateById(preference);
        return toPayload(preference);
    }

    @Transactional
    public void markOnline(Long userId) {
        updatePresence(userId, true);
    }

    @Transactional
    public void markOffline(Long userId) {
        updatePresence(userId, false);
    }

    private void updatePresence(Long userId, boolean online) {
        if (userId == null) return;
        SysUserPreference preference = ensurePreference(userId);
        LocalDateTime now = LocalDateTime.now();
        preference.setConnectedOnline(online ? 1 : 0);
        if (online) {
            preference.setLastOnlineAt(now);
        } else {
            preference.setLastOfflineAt(now);
        }
        preference.setLastActiveAt(now);
        preference.setUpdateTime(now);
        userPreferenceMapper.updateById(preference);
    }

    private SysUserPreference ensurePreference(Long userId) {
        SysUserPreference existing = userPreferenceMapper.selectOne(
                new LambdaQueryWrapper<SysUserPreference>()
                        .eq(SysUserPreference::getUserId, userId)
                        .last("limit 1")
        );
        if (existing != null) {
            if (existing.getTheme() == null || existing.getTheme().isBlank()) {
                existing.setTheme("light");
            }
            if (existing.getViewStateJson() == null || existing.getViewStateJson().isBlank()) {
                existing.setViewStateJson("{}");
            }
            if (existing.getConnectedOnline() == null) {
                existing.setConnectedOnline(0);
            }
            return existing;
        }

        LocalDateTime now = LocalDateTime.now();
        SysUserPreference created = new SysUserPreference();
        created.setUserId(userId);
        created.setTheme("light");
        created.setViewStateJson("{}");
        created.setConnectedOnline(0);
        created.setCreateTime(now);
        created.setUpdateTime(now);
        created.setLastActiveAt(now);
        userPreferenceMapper.insert(created);
        return created;
    }

    private void touchActive(SysUserPreference preference) {
        LocalDateTime now = LocalDateTime.now();
        preference.setLastActiveAt(now);
        preference.setUpdateTime(now);
        userPreferenceMapper.updateById(preference);
    }

    private Map<String, Object> toPayload(SysUserPreference preference) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("theme", normalizeTheme(preference.getTheme()));
        payload.put("lastRoute", preference.getLastRoute());
        payload.put("viewState", readViewState(preference.getViewStateJson()));
        payload.put("connectedOnline", preference.getConnectedOnline() != null && preference.getConnectedOnline() == 1);
        payload.put("lastOnlineAt", preference.getLastOnlineAt());
        payload.put("lastOfflineAt", preference.getLastOfflineAt());
        payload.put("lastActiveAt", preference.getLastActiveAt());
        return payload;
    }

    private String normalizeTheme(String theme) {
        return "dark".equalsIgnoreCase(theme) ? "dark" : "light";
    }

    private String normalizeLastRoute(String route) {
        String value = route == null ? "" : route.trim();
        if (value.isEmpty()) return null;
        return value.length() > 512 ? value.substring(0, 512) : value;
    }

    private String writeViewState(Map<String, Object> viewState) {
        try {
            return OBJECT_MAPPER.writeValueAsString(viewState == null ? Map.of() : viewState);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    private Map<String, Object> readViewState(String json) {
        if (json == null || json.isBlank()) return new LinkedHashMap<>();
        try {
            return OBJECT_MAPPER.readValue(json, VIEW_STATE_TYPE);
        } catch (JsonProcessingException e) {
            return new LinkedHashMap<>();
        }
    }
}
