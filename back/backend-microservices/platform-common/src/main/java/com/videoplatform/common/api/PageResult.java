package com.videoplatform.common.api;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PageResult<T> {
    private List<T> items;
    private long total;
    private long pageNo;
    private long pageSize;
}
