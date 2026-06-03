package com.flex.user_module.api.http.requests;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
/**
 * $DESC
 *
 * @author Yasintha Gunathilake
 * @since 1/15/2026
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AddRole {
    private Integer roleId;
    private String roleName;
    private List<String> permissions;
    private List<Integer> notifications;
}
