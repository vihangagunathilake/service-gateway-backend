package com.flex.user_module.api.DTO.classes;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RolePermissionsDTO {
    private Integer id;
    private String name;
    private List<PermissionDTO> permissions = new ArrayList<>();
    private List<String> notifications = new ArrayList<>();
}
