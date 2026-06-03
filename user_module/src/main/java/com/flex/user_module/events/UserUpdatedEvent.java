package com.flex.user_module.events;

import com.flex.user_module.impl.entities.User;

public record UserUpdatedEvent(
        User user
) {
}
