package com.zs.ytbx.service;

import com.zs.ytbx.dto.NoticePublishRequest;
import com.zs.ytbx.dto.NoticeVO;

import java.util.List;

public interface NoticeService {
    List<NoticeVO> listPublishedNotices();
    void publishNotices(NoticePublishRequest request);
}
