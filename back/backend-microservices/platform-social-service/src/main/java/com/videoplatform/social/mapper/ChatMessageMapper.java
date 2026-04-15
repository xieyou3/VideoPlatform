package com.videoplatform.social.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.videoplatform.social.domain.ChatMessage;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ChatMessageMapper extends BaseMapper<ChatMessage> {
}

