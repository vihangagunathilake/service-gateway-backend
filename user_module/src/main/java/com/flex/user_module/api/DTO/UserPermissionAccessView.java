package com.flex.user_module.api.DTO;

public interface UserPermissionAccessView {
    String getPermission();

    Boolean getAdding();

    Boolean getUpdating();

    Boolean getDeleting();

    Boolean getGetting();

    Boolean getGetAll();

    Boolean getAssigning();

    Boolean getAllowAll();
}
