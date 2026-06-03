package com.flex.notification_module.api.http.DTO;

public interface UserNotificationsList {

    Integer getId();
    String getType();
    Integer getCount();
    String getDescription();
    String getLink();
    boolean isRead();
}
