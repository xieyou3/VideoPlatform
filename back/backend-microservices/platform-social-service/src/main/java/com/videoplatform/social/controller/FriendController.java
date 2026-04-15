package com.videoplatform.social.controller;

import com.videoplatform.common.api.ApiResponse;
import com.videoplatform.social.service.FriendService;
import com.videoplatform.social.vo.response.FriendRequestVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/friends")
@RequiredArgsConstructor
public class FriendController {

    private final FriendService friendService;

    @PostMapping("/request")
    public ApiResponse<Void> sendRequest(@RequestBody Map<String, Long> request) {
        Long senderId = request.get("senderId");
        Long receiverId = request.get("receiverId");
        log.info("发送好友请求: senderId={}, receiverId={}", senderId, receiverId);
        friendService.sendFriendRequest(senderId, receiverId);
        return ApiResponse.success(null);
    }

    @GetMapping("/requests/received")
    public ApiResponse<List<FriendRequestVO>> getReceivedRequests(@RequestParam Long userId) {
        log.info("获取用户 {} 收到的好友请求", userId);
        return ApiResponse.success(friendService.getReceivedRequests(userId));
    }

    @PostMapping("/request/{requestId}/accept")
    public ApiResponse<Void> acceptRequest(@PathVariable Long requestId, @RequestParam Long userId) {
        log.info("用户 {} 接受好友请求 {}", userId, requestId);
        friendService.acceptFriendRequest(requestId, userId);
        return ApiResponse.success(null);
    }

    @PostMapping("/request/{requestId}/reject")
    public ApiResponse<Void> rejectRequest(@PathVariable Long requestId, @RequestParam Long userId) {
        log.info("用户 {} 拒绝好友请求 {}", userId, requestId);
        friendService.rejectFriendRequest(requestId, userId);
        return ApiResponse.success(null);
    }
}
