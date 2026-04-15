package com.videoplatform.social.controller;

import com.videoplatform.common.api.ApiResponse;
import com.videoplatform.social.dto.FriendViewResponse;
import com.videoplatform.social.service.SocialService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/social")
@RequiredArgsConstructor
public class SocialController {

    private final SocialService socialService;

    @GetMapping("/friends")
    public ApiResponse<List<FriendViewResponse>> friends(@RequestHeader(value = "X-User-Id", required = false) Long userId) {
        List<FriendViewResponse> friends = socialService.listFriends(userId == null ? 1L : userId);
        return ApiResponse.success(friends);
    }
}
