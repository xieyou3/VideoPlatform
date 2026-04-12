package com.videoplatform.search.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.videoplatform.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("search_sync_event")
public class SearchSyncEvent extends BaseEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String bizType;
    private Long bizId;
    private String tableName;
    private String operationType;
    private String payloadJson;
    private String syncStatus;
    private Integer retryCount;
}
