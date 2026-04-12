package com.videoplatform.common.security;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class JwtUser {
    private Long userId;
    private String username;
    private List<String> roles;
}
