package com.flex.common_module.security.constants;

/**
 * $DESC
 *
 * @author Yasintha Gunathilake
 * @since 1/13/2026
 */
public class SecurityConstants {
    public static final String SECRET_KEY = "Rhy6W28jBTJvXhRyVxte0zq25hgRsYNc5e1X1vp16eY8qT4W";

    public static final String[] EXCLUDED_PATHS = {
            "/user/login",
            "/user/register",
            "/user/logout",
            "/user/employee-register",
            "/ws/**",
            "/service-gateway/ws/**",
            "/actuator/**"
    };
}
