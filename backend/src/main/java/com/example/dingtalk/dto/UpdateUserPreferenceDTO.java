package com.example.dingtalk.dto;

import lombok.Data;

import java.util.Map;

@Data
public class UpdateUserPreferenceDTO {
    private String theme;
    private String lastRoute;
    private Map<String, Object> viewState;
}
