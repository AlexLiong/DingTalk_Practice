package com.example.dingtalk.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import java.util.List;

@Data
public class CreateGroupDTO {
    private String name;
    @NotEmpty(message = "请至少选择一个成员")
    private List<Long> memberIds;
}
