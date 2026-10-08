package com.fulfillx.orderservice.security;

import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;

public record CurrentUser(Long id, boolean admin) {

    public static CurrentUser from(Jwt jwt) {
        Number uid = jwt.getClaim("uid");
        List<String> roles = jwt.getClaimAsStringList("roles");
        return new CurrentUser(uid.longValue(), roles != null && roles.contains(Role.ADMIN.name()));
    }
}