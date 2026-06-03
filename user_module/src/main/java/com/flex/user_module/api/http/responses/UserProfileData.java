package com.flex.user_module.api.http.responses;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserProfileData {
    private String fName;
    private String lName;
    private String email;
    private String userType;
    private String role;
    private String serviceCenter;
    private String nic;
    private String contact;
    private String joinedDate;
    private String imageUrl;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Notifications {
        private Integer type;
        private String title;
        private String content;
        private boolean disabled;
    }
}
