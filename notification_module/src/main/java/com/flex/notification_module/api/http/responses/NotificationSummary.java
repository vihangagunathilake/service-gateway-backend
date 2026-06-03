package com.flex.notification_module.api.http.responses;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NotificationSummary {
    private Integer count;
    private String description;
    private String link;
    private String type;
}
