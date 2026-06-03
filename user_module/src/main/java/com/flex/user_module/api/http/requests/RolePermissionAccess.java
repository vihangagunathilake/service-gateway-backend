package com.flex.user_module.api.http.requests;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RolePermissionAccess {

    private Integer rolePermissionId;
    private boolean add;
    private boolean update;
    private boolean delete;
    private boolean getAll;
    private boolean get;
    private boolean assign;
    private boolean all;
}
