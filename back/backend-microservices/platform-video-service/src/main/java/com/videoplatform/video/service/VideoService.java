package com.videoplatform.video.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.videoplatform.common.api.PageResult;
import com.videoplatform.video.domain.Tag;
import com.videoplatform.video.domain.Video;
import com.videoplatform.video.domain.VideoComment;
import com.videoplatform.video.domain.VideoDanmaku;
import com.videoplatform.video.domain.VideoInteraction;
import com.videoplatform.video.domain.VideoTag;
import com.videoplatform.video.dto.CommentCreateRequest;
import com.videoplatform.video.dto.DanmakuCreateRequest;
import com.videoplatform.video.dto.VideoDetailResponse;
import com.videoplatform.video.dto.VideoInteractionResponse;
import com.videoplatform.video.dto.VideoListItemResponse;
import com.videoplatform.video.dto.VideoPublishRequest;
import com.videoplatform.video.mapper.TagMapper;
import com.videoplatform.video.mapper.VideoCommentMapper;
import com.videoplatform.video.mapper.VideoDanmakuMapper;
import com.videoplatform.video.mapper.VideoInteractionMapper;
import com.videoplatform.video.mapper.VideoMapper;
import com.videoplatform.video.mapper.VideoTagMapper;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class VideoService {

    private final VideoMapper videoMapper;
    private final VideoDanmakuMapper danmakuMapper;
    private final VideoCommentMapper commentMapper;
    private final VideoInteractionMapper interactionMapper;
    private final VideoTagMapper videoTagMapper;
    private final TagMapper tagMapper;
    private final JdbcTemplate jdbcTemplate;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public PageResult<VideoListItemResponse> listPublishedVideos() {
        List<Video> videos = videoMapper.selectList(new LambdaQueryWrapper<Video>()
                .eq(Video::getStatus, "PUBLISHED")
                .orderByDesc(Video::getCreatedAt));
        Map<Long, String> authorMap = loadAuthorMap(videos.stream().map(Video::getAuthorId).toList());
        List<VideoListItemResponse> items = videos.stream()
                .map(video -> VideoListItemResponse.builder()
                        .id(video.getId())
                        .title(video.getTitle())
                        .thumbnail(video.getCoverUrl())
                        .author(authorMap.getOrDefault(video.getAuthorId(), "未知作者"))
                        .views(video.getViewCount())
                        .duration(formatDuration(video.getDurationSeconds()))
                        .createdAt(video.getCreatedAt().format(DATE_FORMATTER))
                        .likes(video.getLikeCount())
                        .build())
                .toList();
        return PageResult.<VideoListItemResponse>builder().items(items).total(items.size()).pageNo(1).pageSize(items.size()).build();
    }

    public VideoDetailResponse getVideoDetail(Long videoId) {
        Video video = requireVideo(videoId);
        Map<Long, String> authorMap = loadAuthorMap(List.of(video.getAuthorId()));
        return VideoDetailResponse.builder()
                .id(video.getId())
                .title(video.getTitle())
                .description(video.getDescription())
                .author(authorMap.getOrDefault(video.getAuthorId(), "未知作者"))
                .authorAvatar("https://picsum.photos/seed/author" + video.getAuthorId() + "/128")
                .videoUrl(video.getVideoUrl())
                .thumbnail(video.getCoverUrl())
                .durationSeconds(video.getDurationSeconds())
                .views(video.getViewCount())
                .likes(video.getLikeCount())
                .favorites(video.getFavoriteCount())
                .comments(video.getCommentCount())
                .createdAt(video.getCreatedAt().format(DATE_FORMATTER))
                .tags(loadVideoTags(video.getId()))
                .build();
    }

    public List<VideoDetailResponse.DanmakuItem> listDanmaku(Long videoId) {
        return danmakuMapper.selectList(new LambdaQueryWrapper<VideoDanmaku>()
                        .eq(VideoDanmaku::getVideoId, videoId)
                        .orderByAsc(VideoDanmaku::getPlayTimeSeconds))
                .stream()
                .map(item -> VideoDetailResponse.DanmakuItem.builder()
                        .id(item.getId())
                        .text(item.getContent())
                        .time(item.getPlayTimeSeconds())
                        .color(item.getColor())
                        .top((int) ((item.getId() % 70) + 5))
                        .build())
                .toList();
    }

    @Transactional
    public VideoDetailResponse publishVideo(Long userId, VideoPublishRequest request) {
        Video video = new Video();
        video.setAuthorId(userId);
        video.setTitle(request.getTitle());
        video.setDescription(request.getDescription());
        video.setCoverUrl(request.getCoverUrl());
        video.setVideoUrl(request.getVideoUrl());
        video.setDurationSeconds(request.getDurationSeconds() == null ? 0 : request.getDurationSeconds());
        video.setCategory(request.getCategory());
        video.setStatus("PUBLISHED");
        video.setReviewStatus("APPROVED");
        video.setVisibility("PUBLIC");
        video.setViewCount(0L);
        video.setLikeCount(0L);
        video.setFavoriteCount(0L);
        video.setCommentCount(0L);
        video.setDanmakuCount(0L);
        videoMapper.insert(video);
        saveTags(video.getId(), request.getTags());
        return getVideoDetail(video.getId());
    }

    @Transactional
    public void createComment(Long videoId, Long userId, CommentCreateRequest request) {
        requireVideo(videoId);
        VideoComment comment = new VideoComment();
        comment.setVideoId(videoId);
        comment.setUserId(userId);
        comment.setContent(request.getContent());
        comment.setParentId(request.getParentId());
        comment.setLikeCount(0L);
        comment.setStatus("VISIBLE");
        commentMapper.insert(comment);
        jdbcTemplate.update("update video set comment_count = comment_count + 1 where id = ?", videoId);
    }

    @Transactional
    public void createDanmaku(Long videoId, Long userId, DanmakuCreateRequest request) {
        requireVideo(videoId);
        VideoDanmaku danmaku = new VideoDanmaku();
        danmaku.setVideoId(videoId);
        danmaku.setUserId(userId);
        danmaku.setContent(request.getText());
        danmaku.setPlayTimeSeconds(request.getTime());
        danmaku.setColor(request.getColor() == null ? "#FF9800" : request.getColor());
        danmaku.setType("ROLLING");
        danmakuMapper.insert(danmaku);
        jdbcTemplate.update("update video set danmaku_count = danmaku_count + 1 where id = ?", videoId);
    }

    @Transactional
    public VideoInteractionResponse toggleInteraction(Long videoId, Long userId, String type) {
        requireVideo(videoId);
        VideoInteraction existed = interactionMapper.selectOne(new LambdaQueryWrapper<VideoInteraction>()
                .eq(VideoInteraction::getVideoId, videoId)
                .eq(VideoInteraction::getUserId, userId)
                .eq(VideoInteraction::getInteractionType, type));
        boolean active;
        if (existed == null) {
            VideoInteraction interaction = new VideoInteraction();
            interaction.setVideoId(videoId);
            interaction.setUserId(userId);
            interaction.setInteractionType(type);
            interactionMapper.insert(interaction);
            active = true;
            updateCounter(videoId, type, 1);
        } else {
            interactionMapper.deleteById(existed.getId());
            active = false;
            updateCounter(videoId, type, -1);
        }
        Video latest = requireVideo(videoId);
        long count = "LIKE".equals(type) ? latest.getLikeCount() : latest.getFavoriteCount();
        return VideoInteractionResponse.builder().active(active).count(count).build();
    }

    private void updateCounter(Long videoId, String type, int delta) {
        String column = "LIKE".equals(type) ? "like_count" : "favorite_count";
        jdbcTemplate.update("update video set " + column + " = " + column + " + ? where id = ?", delta, videoId);
    }

    private void saveTags(Long videoId, List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return;
        }
        for (String rawTag : tags) {
            String tagName = rawTag.replace("#", "").trim();
            if (tagName.isBlank()) {
                continue;
            }
            Tag tag = tagMapper.selectOne(new LambdaQueryWrapper<Tag>().eq(Tag::getName, tagName));
            if (tag == null) {
                tag = new Tag();
                tag.setName(tagName);
                tagMapper.insert(tag);
            }
            VideoTag relation = new VideoTag();
            relation.setVideoId(videoId);
            relation.setTagId(tag.getId());
            videoTagMapper.insert(relation);
        }
    }

    private List<String> loadVideoTags(Long videoId) {
        List<Long> tagIds = videoTagMapper.selectList(new LambdaQueryWrapper<VideoTag>().eq(VideoTag::getVideoId, videoId))
                .stream().map(VideoTag::getTagId).toList();
        if (tagIds.isEmpty()) {
            return List.of();
        }
        return tagMapper.selectBatchIds(tagIds).stream().map(Tag::getName).map(name -> "#" + name).toList();
    }

    private Map<Long, String> loadAuthorMap(List<Long> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        String inSql = userIds.stream().distinct().map(String::valueOf).collect(Collectors.joining(","));
        return jdbcTemplate.query("select id, username from users where id in (" + inSql + ")", rs -> {
            Map<Long, String> result = new java.util.HashMap<>();
            while (rs.next()) {
                result.put(rs.getLong("id"), rs.getString("username"));
            }
            return result;
        });
    }

    private Video requireVideo(Long videoId) {
        Video video = videoMapper.selectById(videoId);
        if (video == null) {
            throw new IllegalArgumentException("video not found");
        }
        return video;
    }

    private String formatDuration(Integer seconds) {
        int total = seconds == null ? 0 : seconds;
        return String.format("%02d:%02d", total / 60, total % 60);
    }
}
