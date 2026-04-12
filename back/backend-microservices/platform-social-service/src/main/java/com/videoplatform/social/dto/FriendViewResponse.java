package com.videoplatform.social.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FriendViewResponse {
    private Long id;
    private String name;
    private String avatar;
    private String status;
}
