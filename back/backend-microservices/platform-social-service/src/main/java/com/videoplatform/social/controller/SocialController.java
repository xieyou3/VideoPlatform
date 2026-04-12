package com.videoplatform.social.controller;

import com.videoplatform.social.dto.ChatViewResponse;
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

    @GetMapping("/chats")
    public List<ChatViewResponse> chats(@RequestHeader(value = "X-User-Id", required = false) Long userId) {
        return socialService.listChats(userId == null ? 1L : userId);
    }

    @GetMapping("/friends")
    public List<FriendViewResponse> friends(@RequestHeader(value = "X-User-Id", required = false) Long userId) {
        return socialService.listFriends(userId == null ? 1L : userId);
    }
}
