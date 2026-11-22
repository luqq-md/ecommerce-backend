// AuthResponse.java
package com.luqman.ecommerce.auth.dto;

public record AuthResponse(String accessToken, String refreshToken, UserResponse user) {}