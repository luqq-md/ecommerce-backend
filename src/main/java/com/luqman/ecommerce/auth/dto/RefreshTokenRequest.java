// RefreshTokenRequest.java
package com.luqman.ecommerce.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record RefreshTokenRequest(@NotBlank String refreshToken) {}