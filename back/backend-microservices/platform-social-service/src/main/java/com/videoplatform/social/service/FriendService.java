package com.videoplatform.social.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.videoplatform.social.domain.FriendRelation;
import com.videoplatform.social.domain.FriendRequest;
import com.videoplatform.social.mapper.FriendRelationMapper;
import com.videoplatform.social.mapper.FriendRequestMapper;
import com.videoplatform.social.vo.response.FriendRequestVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FriendService {

    private final FriendRequestMapper friendRequestMapper;
    private final FriendRelationMapper friendRelationMapper;
    private final JdbcTemplate jdbcTemplate;

    public void sendFriendRequest(Long senderId, Long receiverId) {
        if (senderId.equals(receiverId)) {
            throw new IllegalArgumentException("不能添加自己为好友");
        }

        FriendRequest existing = friendRequestMapper.selectOne(
                new LambdaQueryWrapper<FriendRequest>()
                        .eq(FriendRequest::getSenderId, senderId)
                        .eq(FriendRequest::getReceiverId, receiverId)
        );

        if (existing != null) {
            throw new IllegalArgumentException("好友请求已发送");
        }

        FriendRequest request = new FriendRequest();
        request.setSenderId(senderId);
        request.setReceiverId(receiverId);
        request.setStatus("PENDING");

        friendRequestMapper.insert(request);
        log.info("用户 {} 向用户 {} 发送好友请求", senderId, receiverId);
    }

    public List<FriendRequestVO> getReceivedRequests(Long userId) {
        List<FriendRequest> requests = friendRequestMapper.selectList(
                new LambdaQueryWrapper<FriendRequest>()
                        .eq(FriendRequest::getReceiverId, userId)
                        .eq(FriendRequest::getStatus, "PENDING")
                        .orderByDesc(FriendRequest::getCreatedAt)
        );

        Map<Long, String> userMap = loadUserMap();

        return requests.stream().map(req -> {
            FriendRequestVO vo = new FriendRequestVO();
            vo.setId(req.getId());
            vo.setSenderId(req.getSenderId());
            vo.setSenderName(userMap.getOrDefault(req.getSenderId(), "用户"));
            vo.setSenderAvatar("https://picsum.photos/seed/user" + req.getSenderId() + "/128");
            vo.setReceiverId(req.getReceiverId());
            vo.setStatus(req.getStatus());
            vo.setCreatedAt(req.getCreatedAt());
            return vo;
        }).collect(Collectors.toList());
    }

    @Transactional
    public void acceptFriendRequest(Long requestId, Long userId) {
        FriendRequest request = friendRequestMapper.selectById(requestId);
        if (request == null || !request.getReceiverId().equals(userId)) {
            throw new IllegalArgumentException("无效的好友请求");
        }

        request.setStatus("ACCEPTED");
        friendRequestMapper.updateById(request);

        FriendRelation relation1 = new FriendRelation();
        relation1.setUserId(request.getSenderId());
        relation1.setFriendUserId(request.getReceiverId());
        relation1.setStatus("ACCEPTED");
        friendRelationMapper.insert(relation1);

        FriendRelation relation2 = new FriendRelation();
        relation2.setUserId(request.getReceiverId());
        relation2.setFriendUserId(request.getSenderId());
        relation2.setStatus("ACCEPTED");
        friendRelationMapper.insert(relation2);

        log.info("用户 {} 接受了用户 {} 的好友请求", userId, request.getSenderId());
    }

    @Transactional
    public void rejectFriendRequest(Long requestId, Long userId) {
        FriendRequest request = friendRequestMapper.selectById(requestId);
        if (request == null || !request.getReceiverId().equals(userId)) {
            throw new IllegalArgumentException("无效的好友请求");
        }

        request.setStatus("REJECTED");
        friendRequestMapper.updateById(request);

        log.info("用户 {} 拒绝了用户 {} 的好友请求", userId, request.getSenderId());
    }

    private Map<Long, String> loadUserMap() {
        return jdbcTemplate.query("select id, username from users", rs -> {
            Map<Long, String> result = new java.util.HashMap<>();
            while (rs.next()) {
                result.put(rs.getLong("id"), rs.getString("username"));
            }
            return result;
        });
    }
}
