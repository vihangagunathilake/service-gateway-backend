package com.flex.user_module.events;

import com.flex.user_module.impl.entities.Role;

import java.util.List;

public record RoleCreatedEvent(
        Integer roleId,
        List<Integer> notificationTypesIds
) {
}
