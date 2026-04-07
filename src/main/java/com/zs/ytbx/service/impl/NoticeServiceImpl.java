package com.zs.ytbx.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zs.ytbx.common.enums.ResultCode;
import com.zs.ytbx.common.exception.BusinessException;
import com.zs.ytbx.dto.NoticePublishRequest;
import com.zs.ytbx.dto.NoticeVO;
import com.zs.ytbx.entity.AxxNoticeEntity;
import com.zs.ytbx.mapper.AxxNoticeMapper;
import com.zs.ytbx.service.NoticeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NoticeServiceImpl implements NoticeService {

    private final AxxNoticeMapper axxNoticeMapper;

    @Override
    public List<NoticeVO> listPublishedNotices() {
        return axxNoticeMapper.selectList(new LambdaQueryWrapper<AxxNoticeEntity>()
                        .orderByAsc(AxxNoticeEntity::getSortNo)
                        .orderByDesc(AxxNoticeEntity::getPublishedAt)
                        .orderByDesc(AxxNoticeEntity::getId))
                .stream()
                .map(this::toNoticeVO)
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void publishNotices(NoticePublishRequest request) {
        if (request.getNotices() == null || request.getNotices().isEmpty()) {
            throw new BusinessException(ResultCode.INVALID_PARAM, "通知列表不能为空");
        }

        LocalDateTime publishedAt = LocalDateTime.now();
        axxNoticeMapper.delete(new LambdaQueryWrapper<AxxNoticeEntity>());

        for (NoticePublishRequest.NoticeItem item : request.getNotices()) {
            String title = item.getTitle() == null ? "" : item.getTitle().trim();
            String content = item.getContent() == null ? "" : item.getContent().trim();

            if (title.isEmpty() || content.isEmpty()) {
                throw new BusinessException(ResultCode.INVALID_PARAM, "通知标题和内容不能为空");
            }

            AxxNoticeEntity entity = new AxxNoticeEntity();
            entity.setTitle(title);
            entity.setContent(content);
            entity.setSortNo(item.getSortNo());
            entity.setPublishedAt(publishedAt);
            entity.setDeleted(0);
            entity.setCreateTime(publishedAt);
            entity.setUpdateTime(publishedAt);
            axxNoticeMapper.insert(entity);
        }
    }

    private NoticeVO toNoticeVO(AxxNoticeEntity entity) {
        return NoticeVO.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .content(entity.getContent())
                .sortNo(entity.getSortNo())
                .publishedAt(entity.getPublishedAt())
                .createTime(entity.getCreateTime())
                .build();
    }
}
