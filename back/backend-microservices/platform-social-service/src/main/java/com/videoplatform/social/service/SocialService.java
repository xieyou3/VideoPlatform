package com.videoplatform.social.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.videoplatform.social.domain.FriendRelation;
import com.videoplatform.social.dto.FriendViewResponse;
import com.videoplatform.social.mapper.FriendRelationMapper;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SocialService {

    private final FriendRelationMapper friendRelationMapper;
    private final JdbcTemplate jdbcTemplate;

    public List<FriendViewResponse> listFriends(Long userId) {
        Map<Long, String> userMap = loadUserMap();
        return friendRelationMapper.selectList(new LambdaQueryWrapper<FriendRelation>()
                        .eq(FriendRelation::getUserId, userId)
                        .eq(FriendRelation::getStatus, "ACCEPTED"))
                .stream()
                .map(friend -> FriendViewResponse.builder()
                        .id(friend.getFriendUserId())
                        .name(userMap.getOrDefault(friend.getFriendUserId(), "好友"))
                        .avatar("https://picsum.photos/seed/friend" + friend.getFriendUserId() + "/128")
                        .status(friend.getFriendUserId() % 2 == 0 ? "online" : "offline")
                        .build())
                .toList();
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
