package com.flex.user_module.api.DTO.classes;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class PermissionDTO {
    private String name;
    private boolean allowed;
}
