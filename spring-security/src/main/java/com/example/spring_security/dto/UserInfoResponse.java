package com.example.spring_security.dto;

import com.example.spring_security.model.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserInfoResponse {
    private int id;
    private String email;
    private String username;
    private User.Role role;
    private User.AuthProvider authProvider;
    private boolean enabled;
} 