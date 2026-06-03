package com.flex.notification_module.api.http.responses;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserNotifications {
    private Integer typeId;
    private String title;
    private String content;
    private  boolean disabled;
}
