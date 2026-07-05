package com.flex.user_module.api.http.requests;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AddUser {
    private Integer userId;
    private String firstName;
    private String lastName;
    private String email;
    private String contact;
    private String nic;
    private int userType;
    private Integer serviceCenterId;
    private Integer roleId;
}
