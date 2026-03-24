package com.zs.ytbx.common.api;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageResponse<T> {

    private List<T> records;

    private long pageNo;

    private long pageSize;

    private long total;

    private long totalPages;

    public static <T> PageResponse<T> of(List<T> source, long pageNo, long pageSize) {
        if (source == null || source.isEmpty()) {
            return PageResponse.<T>builder()
                    .records(Collections.emptyList())
                    .pageNo(pageNo)
                    .pageSize(pageSize)
                    .total(0)
                    .totalPages(0)
                    .build();
        }

        int fromIndex = (int) Math.max(0, (pageNo - 1) * pageSize);
        int toIndex = (int) Math.min(source.size(), fromIndex + pageSize);
        List<T> records = fromIndex >= source.size() ? Collections.emptyList() : source.subList(fromIndex, toIndex);
        long totalPages = (source.size() + pageSize - 1) / pageSize;

        return PageResponse.<T>builder()
                .records(records)
                .pageNo(pageNo)
                .pageSize(pageSize)
                .total(source.size())
                .totalPages(totalPages)
                .build();
    }
}
