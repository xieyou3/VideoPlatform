package com.videoplatform.search.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.videoplatform.search.domain.SearchSyncEvent;
import com.videoplatform.search.mapper.SearchSyncEventMapper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchSyncService {

    private final SearchSyncEventMapper eventMapper;
    private final StringRedisTemplate stringRedisTemplate;

    @Scheduled(cron = "0 */15 * * * ?")
    @Transactional
    public void syncPendingEvents() {
        List<SearchSyncEvent> events = eventMapper.selectList(new LambdaQueryWrapper<SearchSyncEvent>()
                .eq(SearchSyncEvent::getSyncStatus, "PENDING")
                .last("limit 100"));
        for (SearchSyncEvent event : events) {
            try {
                stringRedisTemplate.opsForList().rightPush("search:sync:" + event.getTableName(), event.getPayloadJson());
                event.setSyncStatus("DONE");
            } catch (Exception ex) {
                event.setRetryCount(event.getRetryCount() == null ? 1 : event.getRetryCount() + 1);
                event.setSyncStatus(event.getRetryCount() >= 10 ? "FAILED" : "PENDING");
                log.error("search sync failed, eventId={}", event.getId(), ex);
            }
            eventMapper.updateById(event);
        }
    }
}
