// UserResponse.java
package com.luqman.ecommerce.auth.dto;

import com.luqman.ecommerce.auth.entity.Role;

public record UserResponse(Long id, String email, String fullName, Role role) {}