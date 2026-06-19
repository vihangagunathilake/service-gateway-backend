package com.flex.user_module.api.http.responses;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * $DESC
 *
 * @author Yasintha Gunathilake
 * @since 1/15/2026
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LoginResponse {
    private String token;
    private String refreshToken;
    private boolean password;
    private boolean forgot;
}
