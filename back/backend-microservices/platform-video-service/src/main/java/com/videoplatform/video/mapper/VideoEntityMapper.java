package com.videoplatform.video.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.videoplatform.video.domain.VideoEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface VideoEntityMapper extends BaseMapper<VideoEntity> {
}
