package com.videoplatform.auth.dto;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuthResponse {

    private String token;
    private String refreshToken;
    private UserInfo user;

    @Data
    @Builder
    public static class UserInfo {
        private Long id;
        private String username;
        private String email;
        private String avatar;
        private List<String> roles;
    }
}
