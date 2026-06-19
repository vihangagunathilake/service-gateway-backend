package com.flex.user_module.api.http.requests;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChangePassword {
    private String emailString; //change token which is coming from login as password.
    private String newPassword;
    private String email; // which is coming from login
    private boolean forgot;
}
